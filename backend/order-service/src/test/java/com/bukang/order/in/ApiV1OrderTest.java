package com.bukang.order.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.order.app.OrderExpiryUseCase;
import com.bukang.order.domain.Order;
import com.bukang.order.domain.OrderItem;
import com.bukang.order.domain.OrderStatus;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.ParticipationStatus;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.support.StubRaceCoursePort;
import com.bukang.order.support.StubRaceCoursePortConfig;
import com.bukang.order.support.StubSeatHoldPort;
import com.bukang.order.support.StubSeatHoldPortConfig;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({StubRaceCoursePortConfig.class, StubSeatHoldPortConfig.class})
class ApiV1OrderTest {
	private static final String BASE_URL = "/api/v1/order/orders";
	private static final int MEMBER_ID = 1;
	private static final int OTHER_MEMBER_ID = 2;
	private static final int COURSE_ID = 10;
	private static final int RACE_ID = 100;
	private static final String ORDER_NO_PATTERN = "^OD-\\d{8}-[0-9A-F]{4}$";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ParticipationRepository participationRepository;

	@Autowired
	private StubRaceCoursePort raceCoursePort;

	@Autowired
	private StubSeatHoldPort seatHoldPort;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private OrderExpiryUseCase orderExpiryUseCase;

	@BeforeEach
	void setUp() {
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null));
	}

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
		participationRepository.deleteAll();
		raceCoursePort.clear();
		seatHoldPort.clear();
	}

	@Test
	@DisplayName("[결제하기]하면 자리를 확보해 참가 신청은 APPLIED, 주문은 CREATED가 되고 결제 기한은 10분 뒤다")
	void create() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		LocalDateTime before = LocalDateTime.now();

		create(MEMBER_ID, participationId, newKey())
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.status").value(201))
			.andExpect(jsonPath("$.message").value("자리를 확보했습니다. 결제 기한 안에 결제해주세요."))
			.andExpect(jsonPath("$.data.orderNo").value(matchesPattern(ORDER_NO_PATTERN)))
			.andExpect(jsonPath("$.data.buyerId").value(MEMBER_ID))
			.andExpect(jsonPath("$.data.status").value("CREATED"))
			.andExpect(jsonPath("$.data.items.length()").value(1))
			.andExpect(jsonPath("$.data.items[0].participationId").value(participationId))
			.andExpect(jsonPath("$.data.items[0].quantity").value(1))
			.andExpect(jsonPath("$.data.items[0].status").value("ORDERED"));
		LocalDateTime after = LocalDateTime.now();

		Participation participation = participationRepository.findById(participationId).orElseThrow();
		assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.APPLIED);
		assertThat(participation.getActiveFlag()).isTrue();

		Order order = orderRepository.findAll().getFirst();
		assertThat(order.getPaymentDueAt())
			.isBetween(before.plusMinutes(StubSeatHoldPort.PAYMENT_MINUTES),
				after.plusMinutes(StubSeatHoldPort.PAYMENT_MINUTES))
			.isEqualTo(participation.getPaymentDueAt());
	}

	@Test
	@DisplayName("주문 상품에 정산이 쓰는 판매자 ID·대회 ID·금액(참가비 + 키트)이 저장된다")
	void createStoresSettlementFields() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		int ticketPrice = StubRaceCoursePort.PRICE + StubRaceCoursePort.KIT_PRICE;

		create(MEMBER_ID, participationId, newKey())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.raceId").value(RACE_ID))
			.andExpect(jsonPath("$.data.totalAmount").value(ticketPrice))
			.andExpect(jsonPath("$.data.items[0].sellerId").value(StubRaceCoursePort.SELLER_ID))
			.andExpect(jsonPath("$.data.items[0].raceCourseId").value(COURSE_ID))
			.andExpect(jsonPath("$.data.items[0].itemName").value("테스트 대회 종목 " + COURSE_ID))
			.andExpect(jsonPath("$.data.items[0].unitPrice").value(ticketPrice))
			.andExpect(jsonPath("$.data.items[0].amount").value(ticketPrice));

		// 주문 상품은 지연 로딩이라 트랜잭션 안에서 읽는다
		OrderItem item = transactionTemplate.execute(status ->
			orderRepository.findAll().getFirst().getItems().getFirst());
		assertThat(item.getRaceId()).isEqualTo(RACE_ID);
		assertThat(item.getSellerId()).isEqualTo(StubRaceCoursePort.SELLER_ID);
		assertThat(item.getAmount()).isEqualTo(ticketPrice);
	}

	@Test
	@DisplayName("같은 Idempotency-Key로 다시 보내면 200으로 처음 주문을 돌려주고, 자리는 한 번만 확보한다")
	void createWithSameKey() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		String key = newKey();
		int orderId = createAndGetId(MEMBER_ID, participationId, key);

		create(MEMBER_ID, participationId, key)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("이미 처리한 주문 요청입니다."))
			.andExpect(jsonPath("$.data.id").value(orderId));

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(seatHoldPort.holdCalls()).isEqualTo(1);
	}

	@Test
	@DisplayName("같은 키를 다른 신청서에 다시 쓰면 처음 주문을 돌려주지 않고 409다")
	void reuseKeyForOtherParticipation() throws Exception {
		String key = newKey();
		createAndGetId(MEMBER_ID, applyAndGetId(MEMBER_ID), key);
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID + 1, RACE_ID + 1, 14, null));
		int otherRace = applyAndGetId(MEMBER_ID, COURSE_ID + 1);

		create(MEMBER_ID, otherRace, key)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 다른 주문에 쓴 Idempotency-Key입니다. 새 키로 다시 요청해주세요."));

		assertThat(participationRepository.findById(otherRace).orElseThrow().getStatus())
			.isEqualTo(ParticipationStatus.PENDING);
	}

	@Test
	@DisplayName("다른 키로 같은 신청서를 다시 보내면 이미 결제 대기라 409다")
	void createTwiceWithDifferentKey() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		create(MEMBER_ID, participationId, newKey()).andExpect(status().isCreated());

		create(MEMBER_ID, participationId, newKey())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("결제를 시작할 수 없는 신청입니다(현재 상태: 결제 대기)."));

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(seatHoldPort.holdCalls()).isEqualTo(1);
	}

	@Test
	@DisplayName("남은 자리가 없으면 409, 참가 신청은 FAILED로 끝나 주문이 없고 같은 대회에 다시 신청할 수 있다")
	void soldOut() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		seatHoldPort.setRemainingSeats(COURSE_ID, 0);

		create(MEMBER_ID, participationId, newKey())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value("남은 자리가 없습니다. 참가 신청서를 다시 작성해 신청할 수 있습니다."))
			.andExpect(jsonPath("$.data").isEmpty());

		Participation participation = participationRepository.findById(participationId).orElseThrow();
		assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.FAILED);
		assertThat(participation.getActiveFlag()).isNull();
		assertThat(orderRepository.count()).isZero();

		applyAndGetId(MEMBER_ID);
	}

	@Test
	@DisplayName("마지막 1석에 두 명이 동시에 [결제하기]하면 한 명만 자리를 확보한다")
	void lastSeatRace() throws Exception {
		int first = applyAndGetId(MEMBER_ID);
		int second = applyAndGetId(OTHER_MEMBER_ID);
		seatHoldPort.setRemainingSeats(COURSE_ID, 1);

		CountDownLatch start = new CountDownLatch(1);
		List<Callable<Integer>> requests = List.of(
			() -> createStatusAfter(start, MEMBER_ID, first),
			() -> createStatusAfter(start, OTHER_MEMBER_ID, second));
		List<Integer> statuses = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			List<Future<Integer>> futures = requests.stream().map(executor::submit).toList();
			start.countDown();
			for (Future<Integer> future : futures) {
				statuses.add(future.get());
			}
		}

		assertThat(statuses).containsExactlyInAnyOrder(201, 409);
		assertThat(orderRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("접수 기간이 아니면 409이고 자리를 확보하지 않으며 참가 신청은 PENDING 그대로다")
	void notAccepting() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		raceCoursePort.put(StubRaceCoursePort.closedCourse(COURSE_ID, RACE_ID));

		create(MEMBER_ID, participationId, newKey())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("접수 기간이 아닙니다."));

		assertThat(seatHoldPort.holdCalls()).isZero();
		assertThat(participationRepository.findById(participationId).orElseThrow().getStatus())
			.isEqualTo(ParticipationStatus.PENDING);
	}

	@Test
	@DisplayName("다른 회원의 신청서는 403, 없는 신청서는 404다")
	void notOwnerOrMissing() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);

		create(OTHER_MEMBER_ID, participationId, newKey())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("본인의 참가 신청만 결제할 수 있습니다."));
		create(MEMBER_ID, 999999, newKey())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 참가 신청입니다."));

		assertThat(seatHoldPort.holdCalls()).isZero();
	}

	@Test
	@DisplayName("Idempotency-Key가 없으면 400, 64자를 넘으면 400이다")
	void invalidIdempotencyKey() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);

		mockMvc.perform(post(BASE_URL)
				.header("X-Member-Id", MEMBER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createBody(participationId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("필수 요청 헤더(Idempotency-Key)가 없습니다."));
		create(MEMBER_ID, participationId, "k".repeat(65))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Idempotency-Key는 1~64자로 보내주세요."));
	}

	@Test
	@DisplayName("참가 신청 ID가 없으면 400이다")
	void missingParticipationId() throws Exception {
		mockMvc.perform(post(BASE_URL)
				.header("X-Member-Id", MEMBER_ID)
				.header("Idempotency-Key", newKey())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("결제할 참가 신청을 선택해주세요."));
	}

	@Test
	@DisplayName("만료 정리: 기한 1초 전에는 그대로, 기한이 되면 참가 신청 RELEASED·주문 EXPIRED가 되고 다시 신청할 수 있다")
	void expireOverdue() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		int orderId = createAndGetId(MEMBER_ID, participationId, newKey());
		LocalDateTime due = orderRepository.findById(orderId).orElseThrow().getPaymentDueAt();

		OrderExpiryUseCase.ExpiryResult early = orderExpiryUseCase.expireOverdue(due.minusSeconds(1));
		assertThat(early.releasedParticipations()).isZero();
		assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREATED);

		OrderExpiryUseCase.ExpiryResult onTime = orderExpiryUseCase.expireOverdue(due);
		assertThat(onTime.releasedParticipations()).isEqualTo(1);
		assertThat(onTime.expiredOrders()).isEqualTo(1);
		Participation released = participationRepository.findById(participationId).orElseThrow();
		assertThat(released.getStatus()).isEqualTo(ParticipationStatus.RELEASED);
		assertThat(released.getActiveFlag()).isNull();
		assertThat(released.getReleasedAt()).isEqualTo(due);
		assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.EXPIRED);

		// 같은 사람이 같은 대회에 새 신청서를 쓸 수 있다
		applyAndGetId(MEMBER_ID);
	}

	@Test
	@DisplayName("만료 정리를 두 번 돌려도 결과가 같고, 해제 시각도 바뀌지 않는다")
	void expireOverdueTwice() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		createAndGetId(MEMBER_ID, participationId, newKey());
		LocalDateTime later = LocalDateTime.now().plusMinutes(30);

		orderExpiryUseCase.expireOverdue(later);
		OrderExpiryUseCase.ExpiryResult second = orderExpiryUseCase.expireOverdue(later.plusMinutes(1));

		assertThat(second.releasedParticipations()).isZero();
		assertThat(second.expiredOrders()).isZero();
		assertThat(participationRepository.findById(participationId).orElseThrow().getReleasedAt()).isEqualTo(later);
	}

	@Test
	@DisplayName("만료 정리는 결제 대기 건만 건드리고, 자리 확보 전(PENDING) 신청은 그대로 둔다")
	void expireOverdueSkipsPending() throws Exception {
		int pending = applyAndGetId(MEMBER_ID);

		orderExpiryUseCase.expireOverdue(LocalDateTime.now().plusDays(1));

		Participation participation = participationRepository.findById(pending).orElseThrow();
		assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.PENDING);
		assertThat(participation.getActiveFlag()).isTrue();
	}

	@Test
	@DisplayName("결제 기한을 늘리면 주문·참가 신청의 기한이 함께 2분 늘고, 두 번째 요청은 409다")
	void extendDeadline() throws Exception {
		int participationId = applyAndGetId(MEMBER_ID);
		int orderId = createAndGetId(MEMBER_ID, participationId, newKey());
		LocalDateTime due = orderRepository.findById(orderId).orElseThrow().getPaymentDueAt();
		LocalDateTime extendedDue = due.plusMinutes(StubSeatHoldPort.EXTEND_MINUTES);

		extend(MEMBER_ID, orderId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("결제 기한을 2분 늘렸습니다."))
			.andExpect(jsonPath("$.data.status").value("CREATED"));

		Order order = orderRepository.findById(orderId).orElseThrow();
		assertThat(order.getPaymentDueAt()).isEqualTo(extendedDue);
		assertThat(order.getDeadlineExtendedAt()).isNotNull();
		assertThat(participationRepository.findById(participationId).orElseThrow().getPaymentDueAt())
			.isEqualTo(extendedDue);

		extend(MEMBER_ID, orderId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("결제 기한은 한 번만 늘릴 수 있습니다."));
		assertThat(seatHoldPort.extendCalls()).isEqualTo(1);

		// 늘린 기한 전에는 정리되지 않는다
		orderExpiryUseCase.expireOverdue(due);
		assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREATED);
	}

	@Test
	@DisplayName("기한이 지났거나 이미 만료된 주문은 늘리지 않고 상품 서비스도 부르지 않는다")
	void extendDeadlineAfterDue() throws Exception {
		int orderId = createAndGetId(MEMBER_ID, applyAndGetId(MEMBER_ID), newKey());
		Order order = orderRepository.findById(orderId).orElseThrow();
		ReflectionTestUtils.setField(order, "paymentDueAt", LocalDateTime.now().minusSeconds(1));
		orderRepository.save(order);

		extend(MEMBER_ID, orderId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("결제 기한이 지났습니다."));

		orderExpiryUseCase.expireOverdue(LocalDateTime.now());
		extend(MEMBER_ID, orderId)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("결제 대기 중인 주문만 결제 기한을 늘릴 수 있습니다(현재 상태: 기한 만료)."));
		assertThat(seatHoldPort.extendCalls()).isZero();
	}

	@Test
	@DisplayName("다른 회원의 주문은 403, 없는 주문은 404다")
	void extendDeadlineNotOwnerOrMissing() throws Exception {
		int orderId = createAndGetId(MEMBER_ID, applyAndGetId(MEMBER_ID), newKey());

		extend(OTHER_MEMBER_ID, orderId)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("본인의 주문만 결제 기한을 늘릴 수 있습니다."));
		extend(MEMBER_ID, 999999)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 주문입니다."));
	}

	private ResultActions extend(int memberId, int orderId) throws Exception {
		return mockMvc.perform(post(BASE_URL + "/" + orderId + "/deadline-extension").header("X-Member-Id", memberId));
	}

	private Integer createStatusAfter(CountDownLatch start, int memberId, int participationId) throws Exception {
		start.await();
		return create(memberId, participationId, newKey()).andReturn().getResponse().getStatus();
	}

	private ResultActions create(int memberId, int participationId, String key) throws Exception {
		return mockMvc.perform(post(BASE_URL)
			.header("X-Member-Id", memberId)
			.header("Idempotency-Key", key)
			.contentType(MediaType.APPLICATION_JSON)
			.content(createBody(participationId)));
	}

	private int createAndGetId(int memberId, int participationId, String key) throws Exception {
		String body = create(memberId, participationId, key)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.id");
	}

	private int applyAndGetId(int memberId) throws Exception {
		return applyAndGetId(memberId, COURSE_ID);
	}

	private int applyAndGetId(int memberId, int raceCourseId) throws Exception {
		String body = mockMvc.perform(post("/api/v1/order/participations")
				.header("X-Member-Id", memberId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "raceCourseId": %d, "thirdPartyConsent": true,
						"participant": { "name": "김러너", "birth": "1994-06-25", "gender": "MALE",
							"phone": "010-1234-5678", "emergencyContactName": "김가족",
							"emergencyContactPhone": "010-9876-5432", "tshirtSize": "L" } }
					""".formatted(raceCourseId)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.id");
	}

	private static String createBody(int participationId) {
		return "{\"participationId\": %d}".formatted(participationId);
	}

	private static String newKey() {
		return UUID.randomUUID().toString();
	}
}
