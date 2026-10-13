package com.bukang.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.order.domain.Order;
import com.bukang.order.domain.OrderStatus;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.ParticipationStatus;
import com.bukang.order.domain.exception.OrderNotExtendableException;
import com.bukang.order.domain.exception.OrderNotPayableException;
import com.bukang.order.domain.exception.ParticipationNotCancelableException;
import com.bukang.order.domain.exception.ParticipationNotPayableException;
import com.bukang.order.dto.OrderDto;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.support.StubRaceCoursePort;
import com.bukang.order.support.StubRaceCoursePortConfig;
import com.bukang.order.support.StubSeatHoldPort;
import com.bukang.order.support.StubSeatHoldPortConfig;
import com.bukang.order.support.StubWalletPort;
import com.bukang.order.support.StubWalletPortConfig;
import com.jayway.jsonpath.JsonPath;

/**
 * 결제 기한 직전에 결제·만료 정리·연장·[결제하기]·신청서 취소가 겹쳐도 돈과 자리가 어긋나지 않는지 본다 (PRO-47, 정책 경쟁 상황 테스트)
 * 요청마다 스레드를 따로 띄워 한꺼번에 출발시키고, 시각은 유스케이스에 직접 넣는다. B·D는 스텁으로 응답을 조절한다
 * 끝나면 "결제 성공 = 주문 PAID = 참가 확정 = 예치금 차감"이 주문마다 함께 성립하는지 확인한다
 * H2(MySQL 모드)로 돈다. B·D 실제 API가 나오면(PRO-99) 같은 시나리오를 다시 돌린다
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({StubRaceCoursePortConfig.class, StubSeatHoldPortConfig.class, StubWalletPortConfig.class})
class OrderPayExpiryConcurrencyTest {
	private static final int COURSE_ID = 10;
	private static final int RACE_ID = 100;
	private static final int TICKET_PRICE = StubRaceCoursePort.PRICE;
	private static final int BALANCE = 1_000_000;
	private static final int ORDER_COUNT = 10;
	private static final long TIMEOUT_SECONDS = 30;
	private static final long PAY_DELAY_MILLIS = 20;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ParticipationRepository participationRepository;

	@Autowired
	private OrderCreateUseCase orderCreateUseCase;

	@Autowired
	private OrderPayUseCase orderPayUseCase;

	@Autowired
	private OrderDeadlineExtendUseCase orderDeadlineExtendUseCase;

	@Autowired
	private OrderExpiryUseCase orderExpiryUseCase;

	@Autowired
	private ParticipationApplyUseCase participationApplyUseCase;

	@Autowired
	private StubRaceCoursePort raceCoursePort;

	@Autowired
	private StubSeatHoldPort seatHoldPort;

	@Autowired
	private StubWalletPort walletPort;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null));
		walletPort.setBalance(BALANCE);
	}

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
		participationRepository.deleteAll();
		raceCoursePort.clear();
		seatHoldPort.clear();
		walletPort.clear();
	}

	@Test
	@DisplayName("① 기한 직전 결제와 만료 정리가 동시에 돌면 주문마다 결제·만료 중 하나만 반영되고, 돈이 빠졌는데 자리가 없는 주문이 없다")
	void payJustBeforeDueWhileExpiring() throws Exception {
		List<Integer> orderIds = createOrders(ORDER_COUNT);
		LocalDateTime lastDue = orderIds.stream().map(this::dueOf).max(Comparator.naturalOrder()).orElseThrow();

		List<Callable<Object>> tasks = new ArrayList<>();
		for (int index = 0; index < orderIds.size(); index++) {
			int orderId = orderIds.get(index);
			// 주문마다 자기 기한 1밀리초 전에 결제한다. 상품 서비스 기준으로도 아직 기한 안이다
			LocalDateTime justBefore = dueOf(orderId).minusNanos(1_000_000);
			// 절반은 조금 늦게 출발시켜 만료 정리가 먼저 잠그는 경우도 자주 생기게 한다 (어느 쪽이 이겨도 검증은 같다)
			long delayMillis = index % 2 == 0 ? 0 : PAY_DELAY_MILLIS;
			tasks.add(() -> {
				Thread.sleep(delayMillis);
				return pay(orderId, justBefore);
			});
		}
		// 만료 정리는 모든 주문의 기한이 지난 시각으로 돈다
		tasks.add(() -> orderExpiryUseCase.expireOverdue(lastDue));
		List<Outcome> outcomes = runTogether(tasks);

		int paidCount = 0;
		for (int index = 0; index < orderIds.size(); index++) {
			int orderId = orderIds.get(index);
			Outcome payOutcome = outcomes.get(index);
			if (payOutcome.succeeded()) {
				assertPaid(orderId);
				paidCount++;
			} else {
				// 만료 정리가 먼저 주문을 잠갔다. 결제는 예치금을 건드리기 전에 거절된다
				assertThat(payOutcome.error()).isInstanceOf(OrderNotPayableException.class);
				assertExpiredWithoutCharge(orderId);
			}
		}
		assertThat(outcomes.getLast().succeeded()).isTrue();
		OrderExpiryUseCase.ExpiryResult expiry = (OrderExpiryUseCase.ExpiryResult)outcomes.getLast().value();
		assertThat(expiry.expiredOrders()).isEqualTo(ORDER_COUNT - paidCount);
		assertThat(expiry.releasedParticipations()).isEqualTo(ORDER_COUNT - paidCount);
		assertThat(walletPort.balance()).isEqualTo(BALANCE - TICKET_PRICE * paidCount);
		// 만료가 이긴 주문은 차감을 시도조차 하지 않았다 (차감 뒤 돌려준 것이 아니다)
		assertThat(walletPort.payCalls()).hasSize(paidCount);
		assertThat(walletPort.canceledOrders()).isEmpty();
	}

	@Test
	@DisplayName("② 기한 1초 뒤 결제는 만료 정리와 겹쳐도 거절되고 예치금은 그대로다")
	void payOneSecondAfterDueWhileExpiring() throws Exception {
		int orderId = createOrders(1).getFirst();
		LocalDateTime oneSecondAfter = dueOf(orderId).plusSeconds(1);

		List<Outcome> outcomes = runTogether(List.of(
			() -> pay(orderId, oneSecondAfter),
			() -> orderExpiryUseCase.expireOverdue(oneSecondAfter)));

		assertThat(outcomes.getFirst().error()).isInstanceOf(OrderNotPayableException.class);
		assertThat(outcomes.get(1).succeeded()).isTrue();
		assertExpiredWithoutCharge(orderId);
		assertThat(walletPort.payCalls()).isEmpty();
		assertThat(walletPort.balance()).isEqualTo(BALANCE);
	}

	@Test
	@DisplayName("② 주문의 기한 사본은 남았어도 상품 서비스 기한이 1초 지났으면 결제는 거절되고, 차감한 예치금이 돌아와 그대로다")
	void payAfterSellerDue() throws Exception {
		int orderId = createOrders(1).getFirst();
		int participationId = participationIdOf(orderId);
		// 판정 원본인 상품 서비스 쪽 기한만 1초 전으로 당긴다 (두 서버의 시계 차이, 정리 지연)
		seatHoldPort.expire(participationId);

		assertThatThrownBy(() -> pay(orderId, LocalDateTime.now())).isInstanceOf(OrderNotPayableException.class);

		assertThat(walletPort.canceledOrders()).containsExactly(orderId);
		assertThat(walletPort.chargedOrders()).isEmpty();
		assertThat(walletPort.balance()).isEqualTo(BALANCE);
		assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CREATED);

		// 남은 결제 대기 건은 만료 정리가 끝낸다
		orderExpiryUseCase.expireOverdue(dueOf(orderId));
		assertExpiredWithoutCharge(orderId);
	}

	@Test
	@DisplayName("③ 2분 연장한 주문은 원래 기한이 지난 뒤 결제와 만료 정리가 겹쳐도 결제된다")
	void payDuringExtensionWhileExpiring() throws Exception {
		int orderId = createOrders(1).getFirst();
		LocalDateTime originalDue = dueOf(orderId);
		extend(orderId, LocalDateTime.now());
		assertThat(dueOf(orderId)).isEqualTo(originalDue.plusMinutes(StubSeatHoldPort.EXTEND_MINUTES));
		LocalDateTime afterOriginalDue = originalDue.plusSeconds(1);

		List<Outcome> outcomes = runTogether(List.of(
			() -> pay(orderId, afterOriginalDue),
			() -> orderExpiryUseCase.expireOverdue(afterOriginalDue)));

		assertThat(outcomes.getFirst().succeeded()).isTrue();
		assertThat(outcomes.get(1).value()).isEqualTo(new OrderExpiryUseCase.ExpiryResult(0, 0));
		assertPaid(orderId);
		assertThat(walletPort.balance()).isEqualTo(BALANCE - TICKET_PRICE);
	}

	@Test
	@DisplayName("③ 기한 직전에 연장과 결제가 동시에 들어와도 결제는 성공하고 한 번만 차감된다")
	void extendAndPayTogether() throws Exception {
		List<Integer> orderIds = createOrders(ORDER_COUNT);

		List<Callable<Object>> tasks = new ArrayList<>();
		for (int orderId : orderIds) {
			LocalDateTime justBefore = dueOf(orderId).minusSeconds(1);
			tasks.add(() -> pay(orderId, justBefore));
			tasks.add(() -> extend(orderId, justBefore));
		}
		List<Outcome> outcomes = runTogether(tasks);

		for (int index = 0; index < orderIds.size(); index++) {
			assertThat(outcomes.get(index * 2).succeeded()).isTrue();
			// 결제가 먼저 끝났으면 연장은 "결제 대기 중이 아님"으로 거절된다
			Outcome extendOutcome = outcomes.get(index * 2 + 1);
			if (!extendOutcome.succeeded()) {
				assertThat(extendOutcome.error()).isInstanceOf(OrderNotExtendableException.class);
			}
			assertPaid(orderIds.get(index));
		}
		assertThat(walletPort.payCalls()).hasSize(ORDER_COUNT);
		assertThat(walletPort.balance()).isEqualTo(BALANCE - TICKET_PRICE * ORDER_COUNT);
	}

	@Test
	@DisplayName("④ 마지막 1석에 여러 명이 동시에 [결제하기]하면 한 명만 자리를 확보하고, 나머지 신청은 FAILED로 끝난다")
	void lastSeatRace() throws Exception {
		int contenders = 5;
		List<Integer> participationIds = new ArrayList<>();
		for (int memberId = 1; memberId <= contenders; memberId++) {
			participationIds.add(apply(memberId));
		}
		seatHoldPort.setRemainingSeats(COURSE_ID, 1);

		List<Callable<Object>> tasks = new ArrayList<>();
		for (int index = 0; index < contenders; index++) {
			int memberId = index + 1;
			int participationId = participationIds.get(index);
			tasks.add(() -> transactionTemplate.execute(status -> orderCreateUseCase.create(memberId,
				participationId, UUID.randomUUID().toString(), LocalDateTime.now())));
		}
		List<Outcome> outcomes = runTogether(tasks);
		assertThat(outcomes).allMatch(Outcome::succeeded);

		List<OrderCreateResult> results = outcomes.stream().map(outcome -> (OrderCreateResult)outcome.value()).toList();
		assertThat(results).extracting(OrderCreateResult::outcome)
			.containsOnlyOnce(OrderCreateResult.Outcome.CREATED)
			.containsOnly(OrderCreateResult.Outcome.CREATED, OrderCreateResult.Outcome.SOLD_OUT);
		assertThat(orderRepository.count()).isEqualTo(1);
		List<Participation> participations = participationRepository.findAllById(participationIds);
		assertThat(participations).extracting(Participation::getStatus)
			.containsOnlyOnce(ParticipationStatus.APPLIED)
			.containsOnly(ParticipationStatus.APPLIED, ParticipationStatus.FAILED);

		// 자리를 확보한 한 명은 그대로 결제된다
		int orderId = results.stream()
			.filter(result -> result.outcome() == OrderCreateResult.Outcome.CREATED)
			.findFirst().orElseThrow().order().getId();
		pay(orderId, LocalDateTime.now());
		assertPaid(orderId);
	}

	@Test
	@DisplayName("⑤ 만료 정리가 두 번 동시에 돌아도 주문마다 한 번만 만료되고 멈추지 않는다")
	void expireTwiceTogether() throws Exception {
		List<Integer> orderIds = createOrders(ORDER_COUNT);
		LocalDateTime lastDue = orderIds.stream().map(this::dueOf).max(Comparator.naturalOrder()).orElseThrow();

		List<Outcome> outcomes = runTogether(List.of(
			() -> orderExpiryUseCase.expireOverdue(lastDue),
			() -> orderExpiryUseCase.expireOverdue(lastDue)));
		assertThat(outcomes).allMatch(Outcome::succeeded);

		List<OrderExpiryUseCase.ExpiryResult> results = outcomes.stream()
			.map(outcome -> (OrderExpiryUseCase.ExpiryResult)outcome.value())
			.toList();
		assertThat(results.stream().mapToInt(OrderExpiryUseCase.ExpiryResult::expiredOrders).sum())
			.isEqualTo(ORDER_COUNT);
		assertThat(results.stream().mapToInt(OrderExpiryUseCase.ExpiryResult::releasedParticipations).sum())
			.isEqualTo(ORDER_COUNT);
		for (int orderId : orderIds) {
			assertExpiredWithoutCharge(orderId);
		}
	}

	@Test
	@DisplayName("⑥ 신청서 취소와 [결제하기]가 동시에 들어오면 한쪽만 반영되고, 취소된 신청서로 자리를 잡지 않는다")
	void cancelApplicationWhileStartingPayment() throws Exception {
		List<Integer> participationIds = new ArrayList<>();
		List<Callable<Object>> tasks = new ArrayList<>();
		for (int memberId = 1; memberId <= ORDER_COUNT; memberId++) {
			int participationId = apply(memberId);
			participationIds.add(participationId);
			int finalMemberId = memberId;
			// 절반은 [결제하기]를 조금 늦게 출발시켜 취소가 먼저 잠그는 경우도 자주 생기게 한다
			long delayMillis = memberId % 2 == 0 ? 0 : PAY_DELAY_MILLIS;
			tasks.add(() -> {
				Thread.sleep(delayMillis);
				return transactionTemplate.execute(status -> orderCreateUseCase.create(finalMemberId,
					participationId, UUID.randomUUID().toString(), LocalDateTime.now()));
			});
			tasks.add(() -> transactionTemplate.execute(status ->
				participationApplyUseCase.cancelBeforePayment(participationId, finalMemberId, LocalDateTime.now())));
		}
		List<Outcome> outcomes = runTogether(tasks);

		int createdCount = 0;
		for (int index = 0; index < participationIds.size(); index++) {
			Outcome createOutcome = outcomes.get(index * 2);
			Outcome cancelOutcome = outcomes.get(index * 2 + 1);
			Participation participation = participationRepository.findById(participationIds.get(index)).orElseThrow();
			assertThat(createOutcome.succeeded()).isNotEqualTo(cancelOutcome.succeeded());
			if (createOutcome.succeeded()) {
				createdCount++;
				assertThat(cancelOutcome.error()).isInstanceOf(ParticipationNotCancelableException.class);
				assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.APPLIED);
			} else {
				assertThat(createOutcome.error()).isInstanceOf(ParticipationNotPayableException.class);
				assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
				assertThat(participation.getActiveFlag()).isNull();
			}
		}
		// 취소된 신청서로는 상품 서비스에 자리를 요청하지 않고, 주문도 만들지 않는다
		assertThat(seatHoldPort.holdCalls()).isEqualTo(createdCount);
		assertThat(orderRepository.count()).isEqualTo(createdCount);
	}

	// 결제 성공 = 주문 PAID = 참가 확정 = 자리 판매 = 예치금이 그 주문으로 차감된 채 남아 있음
	private void assertPaid(int orderId) {
		assertThat(statusOf(orderId)).isEqualTo(OrderStatus.PAID);
		int participationId = participationIdOf(orderId);
		Participation participation = participationRepository.findById(participationId).orElseThrow();
		assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
		assertThat(seatHoldPort.isSold(participationId)).isTrue();
		assertThat(walletPort.chargedOrders()).contains(orderId);
	}

	// 만료 = 주문 EXPIRED = 참가 신청 RELEASED(같은 대회에 다시 신청 가능) = 자리 판매 없음 = 그 주문으로 차감된 예치금 없음
	private void assertExpiredWithoutCharge(int orderId) {
		assertThat(statusOf(orderId)).isEqualTo(OrderStatus.EXPIRED);
		int participationId = participationIdOf(orderId);
		Participation participation = participationRepository.findById(participationId).orElseThrow();
		assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.RELEASED);
		assertThat(participation.getActiveFlag()).isNull();
		assertThat(seatHoldPort.isSold(participationId)).isFalse();
		assertThat(walletPort.chargedOrders()).doesNotContain(orderId);
	}

	// 컨트롤러와 같이 한 트랜잭션에서 결제한다 (OrderFacade.pay). 시각만 테스트가 정한다
	// 회원 ID는 트랜잭션 밖에서 구한다. 안에서 주문을 먼저 읽으면 잠금 조회가 그 옛 상태를 그대로 돌려준다
	private OrderPayResult pay(int orderId, LocalDateTime now) {
		int memberId = memberOf(orderId);
		return transactionTemplate.execute(status -> orderPayUseCase.pay(memberId, orderId, now));
	}

	// 결제와 같은 방식으로 결제 기한을 늘린다 (OrderFacade.extendDeadline)
	private OrderDto extend(int orderId, LocalDateTime now) {
		int memberId = memberOf(orderId);
		return transactionTemplate.execute(status -> orderDeadlineExtendUseCase.extend(memberId, orderId, now));
	}

	/**
	 * 작업들을 스레드마다 하나씩 띄워 한꺼번에 출발시키고, 넣은 순서대로 결과나 예외를 돌려준다
	 * 잠금을 기다리다 멈추면(교착) 시간 초과로 테스트가 실패한다. 끝나지 않은 작업은 중단시켜 테스트가 멈추지 않게 한다
	 */
	private List<Outcome> runTogether(List<Callable<Object>> tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		List<Outcome> outcomes = new ArrayList<>();
		ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
		try {
			List<Future<Object>> futures = tasks.stream()
				.map(task -> executor.submit(() -> {
					start.await();
					return task.call();
				}))
				.toList();
			start.countDown();
			for (Future<Object> future : futures) {
				try {
					outcomes.add(Outcome.success(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)));
				} catch (ExecutionException exception) {
					outcomes.add(Outcome.failure(exception.getCause()));
				} catch (TimeoutException exception) {
					throw new AssertionError("%d초 안에 끝나지 않았습니다 (잠금 교착 의심)".formatted(TIMEOUT_SECONDS), exception);
				}
			}
		} finally {
			executor.shutdownNow();
		}
		return outcomes;
	}

	// 회원마다 신청서 작성 → [결제하기]로 결제 대기 주문을 만든다. 회원 ID는 1부터 차례로 쓴다
	private List<Integer> createOrders(int count) throws Exception {
		List<Integer> orderIds = new ArrayList<>();
		for (int memberId = 1; memberId <= count; memberId++) {
			int participationId = apply(memberId);
			int finalMemberId = memberId;
			OrderCreateResult result = transactionTemplate.execute(status -> orderCreateUseCase.create(finalMemberId,
				participationId, UUID.randomUUID().toString(), LocalDateTime.now()));
			orderIds.add(result.order().getId());
		}
		return orderIds;
	}

	private int apply(int memberId) throws Exception {
		String body = mockMvc.perform(post("/api/v1/order/participations")
				.header("X-Member-Id", memberId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "raceCourseId": %d, "thirdPartyConsent": true,
						"participant": { "name": "김러너", "birth": "1994-06-25", "gender": "MALE",
							"phone": "010-1234-5678", "emergencyContactName": "김가족",
							"emergencyContactPhone": "010-9876-5432", "tshirtSize": "L",
						"shippingAddress": { "recipientName": "김러너", "recipientPhone": "010-1234-5678",
							"zipcode": "04700", "address": "서울 성동구 왕십리로 00", "addressDetail": "101동 1001호" } } }
					""".formatted(COURSE_ID)))
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.id");
	}

	private Order orderOf(int orderId) {
		return orderRepository.findById(orderId).orElseThrow();
	}

	private OrderStatus statusOf(int orderId) {
		return orderOf(orderId).getStatus();
	}

	// DB에 저장된 기한 (저장하며 잘린 정밀도 그대로)
	private LocalDateTime dueOf(int orderId) {
		return orderOf(orderId).getPaymentDueAt();
	}

	private int memberOf(int orderId) {
		return orderOf(orderId).getBuyerId();
	}

	private int participationIdOf(int orderId) {
		// 주문 상품은 지연 로딩이라 트랜잭션 안에서 읽는다
		return transactionTemplate.execute(status -> orderOf(orderId).participationId());
	}

	private record Outcome(Object value, Throwable error) {
		static Outcome success(Object value) {
			return new Outcome(value, null);
		}

		static Outcome failure(Throwable error) {
			return new Outcome(null, error);
		}

		boolean succeeded() {
			return error == null;
		}
	}
}
