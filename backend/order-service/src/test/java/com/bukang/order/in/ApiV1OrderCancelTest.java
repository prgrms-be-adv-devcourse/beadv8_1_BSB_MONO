package com.bukang.order.in;

import static com.bukang.order.out.race.RaceCourseInfo.SCHEDULE_CHANGE_REFUND_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.order.domain.Order;
import com.bukang.order.domain.OrderItem;
import com.bukang.order.domain.OrderStatus;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.ParticipationStatus;
import com.bukang.order.out.OrderOutboxKafkaEventRepository;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.out.QueueAdmissionRepository;
import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.support.QueueEntry;
import com.bukang.order.support.SeatOutboxMessages;
import com.bukang.order.support.StubRaceCoursePort;
import com.bukang.order.support.StubRaceCoursePortConfig;
import com.bukang.order.support.StubRefundEligibilityPort;
import com.bukang.order.support.StubRefundEligibilityPortConfig;
import com.bukang.order.support.StubSeatHoldPort;
import com.bukang.order.support.StubSeatHoldPortConfig;
import com.bukang.order.support.StubWalletPort;
import com.bukang.order.support.StubWalletPortConfig;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({StubRaceCoursePortConfig.class, StubSeatHoldPortConfig.class, StubWalletPortConfig.class,
	StubRefundEligibilityPortConfig.class})
class ApiV1OrderCancelTest {
	private static final String BASE_URL = "/api/v1/order/orders";
	private static final int MEMBER_ID = 1;
	private static final int OTHER_MEMBER_ID = 2;
	private static final int COURSE_ID = 10;
	private static final int RACE_ID = 100;
	private static final int TICKET_PRICE = StubRaceCoursePort.PRICE;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ParticipationRepository participationRepository;

	@Autowired
	private QueueAdmissionRepository queueAdmissionRepository;

	@Autowired
	private StubRaceCoursePort raceCoursePort;

	@Autowired
	private StubSeatHoldPort seatHoldPort;

	@Autowired
	private OrderOutboxKafkaEventRepository orderOutboxKafkaEventRepository;

	@Autowired
	private StubWalletPort walletPort;

	@Autowired
	private StubRefundEligibilityPort refundEligibilityPort;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		orderOutboxKafkaEventRepository.deleteAll();
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null));
	}

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
		participationRepository.deleteAll();
		queueAdmissionRepository.deleteAll();
		raceCoursePort.clear();
		seatHoldPort.clear();
		orderOutboxKafkaEventRepository.deleteAll();
		walletPort.clear();
		refundEligibilityPort.clear();
	}

	@Test
	@DisplayName("환불 기한 당일에 취소하면 자리 되돌리기·예치금 환불이 한 번씩 일어나고 주문·참가 신청이 CANCELED가 된다")
	void cancelOnRefundDeadline() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		setRefundDeadline(LocalDate.now());

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("취소했습니다. 예치금 %,d원을 돌려드렸습니다.".formatted(TICKET_PRICE)))
			.andExpect(jsonPath("$.data.status").value("CANCELED"))
			.andExpect(jsonPath("$.data.canceledAmount").value(TICKET_PRICE))
			.andExpect(jsonPath("$.data.items[0].status").value("CANCELED"));

		assertThat(restoredSeats()).containsExactly(paid.participationId());
		assertThat(walletPort.refundCalls()).containsExactly(new StubWalletPort.Call(paid.orderId(), MEMBER_ID));
		assertThat(walletPort.refundedAmounts()).containsExactly(TICKET_PRICE);
		Participation participation = participationRepository.findById(paid.participationId()).orElseThrow();
		assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
		assertThat(participation.getActiveFlag()).isNull();
		assertThat(participation.getCanceledAt()).isNotNull();
		assertThat(orderRepository.findById(paid.orderId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELED);

		// 취소한 참가권은 정산용 판매 내역에서 빠지고, 같은 대회에 다시 신청할 수 있다
		int raceId = raceCoursePort.findCourse(COURSE_ID).orElseThrow().raceId();
		mockMvc.perform(get("/internal/v1/order/sold-entries").param("raceId", String.valueOf(raceId)))
			.andExpect(jsonPath("$.data.length()").value(0));
		applyAndGetId(MEMBER_ID);
	}

	@Test
	@DisplayName("환불 기한 다음 날에는 409로 거절되고 아무것도 바뀌지 않는다")
	void cancelAfterRefundDeadline() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		LocalDate yesterday = LocalDate.now().minusDays(1);
		setRefundDeadline(yesterday);

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("환불 기한(%s)이 지나 취소할 수 없습니다.".formatted(yesterday)));

		assertNothingCanceled(paid);
	}

	@Test
	@DisplayName("판매자가 대회를 취소했으면 환불 기한이 지났어도 결제한 참가권을 취소하면 전액 환불된다")
	void cancelCanceledRaceAfterRefundDeadline() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		raceCoursePort.put(StubRaceCoursePort.canceled(
			StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null, LocalDate.now().minusDays(10))));

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.canceledAmount").value(TICKET_PRICE));

		assertThat(walletPort.refundedAmounts()).containsExactly(TICKET_PRICE);
	}

	@Test
	@DisplayName("환불 기한이 지났어도 대회 일정이 바뀐 날(알린 날)부터 7일째 날까지는 취소하면 전액 환불된다")
	void cancelWithinScheduleChangeRefund() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		// 6일 전에 바뀌었다 → 바뀐 날을 1일째로 세면 오늘이 7일째 날
		setRefundDeadline(LocalDate.now().minusDays(10), LocalDateTime.now().minusDays(6));

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("취소했습니다. 예치금 %,d원을 돌려드렸습니다.".formatted(TICKET_PRICE)))
			.andExpect(jsonPath("$.data.canceledAmount").value(TICKET_PRICE));

		assertThat(restoredSeats()).containsExactly(paid.participationId());
		assertThat(walletPort.refundedAmounts()).containsExactly(TICKET_PRICE);
	}

	@Test
	@DisplayName("일정 변경 뒤 8일째 날에는 환불 기한도 지났으면 409로 거절되고 아무것도 바뀌지 않는다")
	void cancelAfterScheduleChangeRefund() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		LocalDate refundDeadline = LocalDate.now().minusDays(10);
		// 7일 전에 바뀌었다 → 오늘이 8일째 날
		LocalDateTime changedAt = LocalDateTime.now().minusDays(7);
		setRefundDeadline(refundDeadline, changedAt);

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("환불 기한(%s)과 일정 변경 뒤 전액 환불 기간(%s까지)이 지나 취소할 수 없습니다."
				.formatted(refundDeadline, changedAt.toLocalDate().plusDays(SCHEDULE_CHANGE_REFUND_DAYS - 1))));

		assertNothingCanceled(paid);
	}

	@Test
	@DisplayName("일정 변경 7일이 환불 기한보다 먼저 끝났으면 거절 메시지에는 환불 기한만 나온다")
	void cancelAfterRefundDeadlineWithOldScheduleChange() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		LocalDate refundDeadline = LocalDate.now().minusDays(2);
		setRefundDeadline(refundDeadline, LocalDateTime.now().minusDays(20));

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("환불 기한(%s)이 지나 취소할 수 없습니다.".formatted(refundDeadline)));
	}

	@Test
	@DisplayName("대회가 미뤄져도 정산은 주문 때 복사한 옛 대회 날짜로 하므로, 그 날짜로 정산이 시작됐으면 7일 안이어도 거절한다")
	void cancelPostponedRaceAlreadySettled() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		RaceCourseInfo before = raceCoursePort.findCourse(COURSE_ID).orElseThrow();
		reschedule(before, before.raceDate().plusDays(14));
		refundEligibilityPort.settle(before.raceDate().toLocalDate());

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("정산이 시작된 참가권이라 취소할 수 없습니다."))
			.andExpect(jsonPath("$.code").value("ORDER_PAYOUT_STARTED"));

		assertNothingCanceled(paid);
	}

	@Test
	@DisplayName("대회가 당겨져 바뀐 대회 날짜로 정산이 시작됐으면, 주문 때 복사한 날짜로는 아직이어도 거절한다")
	void cancelAdvancedRaceAlreadySettled() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		RaceCourseInfo before = raceCoursePort.findCourse(COURSE_ID).orElseThrow();
		LocalDateTime advanced = before.raceDate().minusDays(14);
		reschedule(before, advanced);
		refundEligibilityPort.settle(advanced.toLocalDate());

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("정산이 시작된 참가권이라 취소할 수 없습니다."))
			.andExpect(jsonPath("$.code").value("ORDER_PAYOUT_STARTED"));

		assertNothingCanceled(paid);
	}

	@Test
	@DisplayName("대회가 미뤄졌고 두 날짜 모두 정산 전이면 7일 안 취소는 환불되고, 정산 여부는 두 날짜로 모두 묻는다")
	void cancelPostponedRaceBeforePayout() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		RaceCourseInfo before = raceCoursePort.findCourse(COURSE_ID).orElseThrow();
		LocalDateTime postponed = before.raceDate().plusDays(14);
		reschedule(before, postponed);

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId()).andExpect(status().isOk());

		assertThat(refundEligibilityPort.askedRaceDates())
			.containsExactly(before.raceDate().toLocalDate(), postponed.toLocalDate());
	}

	@Test
	@DisplayName("정산 서비스에는 주문을 잠그기 전, DB 트랜잭션 밖에서 묻는다 (정산이 느려도 잠금·커넥션을 붙잡지 않는다)")
	void asksPayoutOutsideTransaction() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId()).andExpect(status().isOk());

		assertThat(refundEligibilityPort.askedInTransaction()).isNotEmpty().containsOnly(false);
	}

	@Test
	@DisplayName("같은 상품 취소를 두 번 보내면 두 번째는 409이고 환불은 한 번만 일어난다")
	void cancelTwice() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId()).andExpect(status().isOk());

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 취소한 상품입니다."))
			.andExpect(jsonPath("$.code").value("ORDER_ITEM_NOT_CANCELABLE"));

		assertThat(walletPort.refundCalls()).hasSize(1);
		assertThat(restoredSeats()).hasSize(1);
	}

	@Test
	@DisplayName("정산 서비스가 환불할 수 없다고 하면(정산 대상일 지남) 409로 거절된다")
	void cancelAfterPayoutTargetDate() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		refundEligibilityPort.setRefundable(false);

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("정산이 시작된 참가권이라 취소할 수 없습니다."))
			.andExpect(jsonPath("$.code").value("ORDER_PAYOUT_STARTED"));

		assertNothingCanceled(paid);
	}

	@Test
	@DisplayName("결제 전 주문의 상품은 취소할 수 없다")
	void cancelUnpaid() throws Exception {
		int orderId = createdOrder(MEMBER_ID);
		int orderItemId = itemOf(orderId).getId();

		cancel(MEMBER_ID, orderId, orderItemId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("결제한 상품만 취소할 수 있습니다(현재 상태: 결제 대기)."))
			.andExpect(jsonPath("$.code").value("ORDER_ITEM_NOT_CANCELABLE"));
	}

	@Test
	@DisplayName("취소와 참가자 정보 수정이 동시에 들어와도 CANCELED와 고친 정보가 서로 덮이지 않는다")
	void cancelWhileChangingParticipant() throws Exception {
		int rounds = 5;
		for (int memberId = 1; memberId <= rounds; memberId++) {
			Paid paid = paidOrder(memberId);
			int finalMemberId = memberId;

			CountDownLatch start = new CountDownLatch(1);
			List<Callable<Integer>> requests = List.of(
				() -> {
					start.await();
					return cancel(finalMemberId, paid.orderId(), paid.orderItemId())
						.andReturn().getResponse().getStatus();
				},
				() -> {
					start.await();
					return changeParticipantName(finalMemberId, paid.participationId(), "박러너")
						.andReturn().getResponse().getStatus();
				});
			List<Integer> statuses = new ArrayList<>();
			try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
				List<Future<Integer>> futures = requests.stream().map(executor::submit).toList();
				start.countDown();
				for (Future<Integer> future : futures) {
					statuses.add(future.get());
				}
			}

			// 취소는 늘 성공한다. 정보 수정은 취소보다 먼저 끝났으면 200, 뒤면 끝난 신청이라 409다
			assertThat(statuses.getFirst()).isEqualTo(200);
			assertThat(statuses.get(1)).isIn(200, 409);
			Participation participation = participationRepository.findById(paid.participationId()).orElseThrow();
			assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
			assertThat(participation.getActiveFlag()).isNull();
			// 정보 수정이 성공했으면 고친 이름도 남아 있어야 한다 (취소가 옛 값으로 덮지 않음)
			if (statuses.get(1) == 200) {
				assertThat(participation.getParticipant().getName()).isEqualTo("박러너");
			}
		}
	}

	@Test
	@DisplayName("예치금 환불이 실패하면(503) 취소가 롤백되어 주문은 결제 완료로 남고, 자리 복구도 남지 않아 "
		+ "정원보다 더 팔리지 않는다")
	void refundFailureLeavesNoSeatRestore() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);
		setRefundDeadline(LocalDate.now());
		walletPort.setRefundFails(true);

		cancel(MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isServiceUnavailable());

		assertThat(restoredSeats()).isEmpty();
		assertThat(orderRepository.findById(paid.orderId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(participationRepository.findById(paid.participationId()).orElseThrow().getStatus())
			.isEqualTo(ParticipationStatus.CONFIRMED);
	}

	@Test
	@DisplayName("다른 회원의 주문은 403, 없는 주문·주문에 없는 상품은 404다")
	void cancelNotOwnerOrMissing() throws Exception {
		Paid paid = paidOrder(MEMBER_ID);

		cancel(OTHER_MEMBER_ID, paid.orderId(), paid.orderItemId())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("본인의 주문만 취소할 수 있습니다."))
			.andExpect(jsonPath("$.code").value("ORDER_FORBIDDEN"));
		cancel(MEMBER_ID, 999999, paid.orderItemId())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 주문입니다."))
			.andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
		cancel(MEMBER_ID, paid.orderId(), 999999)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("주문에 없는 상품입니다."))
			.andExpect(jsonPath("$.code").value("ORDER_ITEM_NOT_FOUND"));
		assertNothingCanceled(paid);
	}

	private void assertNothingCanceled(Paid paid) {
		assertThat(walletPort.refundCalls()).isEmpty();
		assertThat(restoredSeats()).isEmpty();
		assertThat(orderRepository.findById(paid.orderId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(participationRepository.findById(paid.participationId()).orElseThrow().getStatus())
			.isEqualTo(ParticipationStatus.CONFIRMED);
	}

	private void setRefundDeadline(LocalDate refundDeadline) {
		RaceCourseInfo course = StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null, refundDeadline);
		raceCoursePort.put(course);
	}

	// 환불 기한이 지난 뒤 대회 일시를 바꿔, 일정 변경 뒤 7일 전액 환불 기간 안으로 만든다
	private void reschedule(RaceCourseInfo before, LocalDateTime newRaceDate) {
		raceCoursePort.put(new RaceCourseInfo(before.raceCourseId(), before.raceId(), newRaceDate,
			before.applicationOpenAt(), before.applicationCloseAt(), before.minAge(), before.maxAge(),
			before.sellerId(), before.courseName(), before.price(), before.kitIncluded(),
			LocalDate.now().minusDays(1),
			LocalDateTime.now().minusDays(1)));
	}

	private void setRefundDeadline(LocalDate refundDeadline, LocalDateTime scheduleChangedAt) {
		raceCoursePort.put(
			StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null, refundDeadline, scheduleChangedAt));
	}

	private ResultActions cancel(int memberId, int orderId, int orderItemId) throws Exception {
		return mockMvc.perform(post(BASE_URL + "/" + orderId + "/items/" + orderItemId + "/cancel")
			.header("X-Member-Id", memberId));
	}

	private ResultActions changeParticipantName(int memberId, int participationId, String name) throws Exception {
		return mockMvc.perform(put("/api/v1/order/participations/" + participationId + "/participant")
			.header("X-Member-Id", memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{ "name": "%s", "birth": "1994-06-25", "gender": "MALE", "phone": "010-1234-5678",
					"emergencyContactName": "김가족", "emergencyContactPhone": "010-9876-5432", "tshirtSize": "L",
						"shippingAddress": { "recipientName": "김러너", "recipientPhone": "010-1234-5678",
							"zipcode": "04700", "address": "서울 성동구 왕십리로 00", "addressDetail": "101동 1001호" } }
				""".formatted(name)));
	}

	private record Paid(int orderId, int orderItemId, int participationId) {
	}

	// 신청서 → [결제하기] → 결제까지 마친 주문
	private Paid paidOrder(int memberId) throws Exception {
		int orderId = createdOrder(memberId);
		mockMvc.perform(post(BASE_URL + "/" + orderId + "/payment").header("X-Member-Id", memberId))
			.andExpect(status().isOk());
		OrderItem item = itemOf(orderId);
		return new Paid(orderId, item.getId(), item.getParticipationId());
	}

	private OrderItem itemOf(int orderId) {
		return transactionTemplate.execute(status -> {
			Order order = orderRepository.findById(orderId).orElseThrow();
			return order.getItems().getFirst();
		});
	}

	private int createdOrder(int memberId) throws Exception {
		int participationId = applyAndGetId(memberId);
		return read(mockMvc.perform(post(BASE_URL)
			.header("X-Member-Id", memberId)
			.header("Idempotency-Key", UUID.randomUUID().toString())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"participationId\": %d}".formatted(participationId))));
	}

	private int applyAndGetId(int memberId) throws Exception {
		QueueEntry.enter(mockMvc, memberId, COURSE_ID);
		return read(mockMvc.perform(post("/api/v1/order/participations")
			.header("X-Member-Id", memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{ "raceCourseId": %d, "thirdPartyConsent": true,
					"participant": { "name": "김러너", "birth": "1994-06-25", "gender": "MALE",
						"phone": "010-1234-5678", "emergencyContactName": "김가족",
						"emergencyContactPhone": "010-9876-5432", "tshirtSize": "L",
						"shippingAddress": { "recipientName": "김러너", "recipientPhone": "010-1234-5678",
							"zipcode": "04700", "address": "서울 성동구 왕십리로 00", "addressDetail": "101동 1001호" } } }
				""".formatted(COURSE_ID))));
	}

	private int read(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id");
	}

	private List<Integer> restoredSeats() {
		return SeatOutboxMessages.restored(orderOutboxKafkaEventRepository);
	}

	private List<Integer> releasedSeats() {
		return SeatOutboxMessages.released(orderOutboxKafkaEventRepository);
	}
}
