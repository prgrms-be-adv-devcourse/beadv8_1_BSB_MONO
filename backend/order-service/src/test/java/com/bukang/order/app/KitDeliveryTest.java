package com.bukang.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
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

import com.bukang.order.domain.KitDeliveryStatus;
import com.bukang.order.domain.Order;
import com.bukang.order.domain.OrderItem;
import com.bukang.order.domain.OrderItemStatus;
import com.bukang.order.domain.exception.KitNotShippableException;
import com.bukang.order.domain.exception.KitShippingInfoInvalidException;
import com.bukang.order.domain.exception.OrderAccessDeniedException;
import com.bukang.order.domain.exception.OrderItemNotFoundException;
import com.bukang.order.dto.OrderItemDto;
import com.bukang.order.dto.SellerKitDto;
import com.bukang.order.dto.SellerKitSummaryDto;
import com.bukang.order.out.OrderRepository;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.support.StubRaceCoursePort;
import com.bukang.order.support.StubRaceCoursePortConfig;
import com.bukang.order.support.StubRefundEligibilityPortConfig;
import com.bukang.order.support.StubSeatHoldPort;
import com.bukang.order.support.StubSeatHoldPortConfig;
import com.bukang.order.support.StubWalletPort;
import com.bukang.order.support.StubWalletPortConfig;
import com.jayway.jsonpath.JsonPath;

/**
 * 판매자가 키트를 발송 처리하고, 구매자 주문 조회에 배송 상태가 보이는지 본다 (PRO-136)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({StubRaceCoursePortConfig.class, StubSeatHoldPortConfig.class, StubWalletPortConfig.class,
	StubRefundEligibilityPortConfig.class})
class KitDeliveryTest {
	private static final int COURSE_ID = 40;
	private static final int OPEN_COURSE_ID = 41;
	private static final String CARRIER = "CJ대한통운";
	private static final String TRACKING_NUMBER = "684912345678";
	private static final int RACE_ID = 400;
	private static final int MEMBER_ID = 1;
	private static final int SELLER_ID = StubRaceCoursePort.SELLER_ID;
	private static final int OTHER_SELLER_ID = SELLER_ID + 1;

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
	private KitDeliveryUseCase kitDeliveryUseCase;

	@Autowired
	private StubRaceCoursePort raceCoursePort;

	@Autowired
	private StubSeatHoldPort seatHoldPort;

	@Autowired
	private StubWalletPort walletPort;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
		participationRepository.deleteAll();
		raceCoursePort.clear();
		seatHoldPort.clear();
		walletPort.clear();
	}

	@Test
	@DisplayName("결제한 키트는 배송 준비 → 배송 중(송장) → 배송 완료로 바뀌고, 구매자 주문 조회에 그대로 보인다")
	void shipAndDeliverAfterRefundDeadline() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));
		int orderId = paidOrder();
		int itemId = itemIdOf(orderId);
		findOrder(orderId)
			.andExpect(jsonPath("$.data.items[0].kitDeliveryStatus").value("PREPARING"))
			.andExpect(jsonPath("$.data.items[0].kitTrackingNumber").isEmpty())
			.andExpect(jsonPath("$.data.items[0].kitShippedAt").isEmpty());

		LocalDateTime shippedAt = LocalDateTime.now();
		OrderItemDto shipped = kitDeliveryUseCase.ship(SELLER_ID, itemId, CARRIER, TRACKING_NUMBER, shippedAt);

		assertThat(shipped.getKitDeliveryStatus()).isEqualTo(KitDeliveryStatus.SHIPPING);
		findOrder(orderId)
			.andExpect(jsonPath("$.data.items[0].kitDeliveryStatus").value("SHIPPING"))
			.andExpect(jsonPath("$.data.items[0].kitCarrier").value(CARRIER))
			.andExpect(jsonPath("$.data.items[0].kitTrackingNumber").value(TRACKING_NUMBER))
			.andExpect(jsonPath("$.data.items[0].kitDeliveredAt").isEmpty());
		assertThat(itemOf(orderId).getKitShippedAt()).isEqualTo(shippedAt);

		LocalDateTime deliveredAt = shippedAt.plusDays(2);
		kitDeliveryUseCase.completeDelivery(SELLER_ID, itemId, deliveredAt);

		findOrder(orderId)
			.andExpect(jsonPath("$.data.items[0].kitDeliveryStatus").value("DELIVERED"))
			.andExpect(jsonPath("$.data.items[0].kitDeliveredAt").isNotEmpty());
		assertThat(itemOf(orderId).getKitDeliveredAt()).isEqualTo(deliveredAt);
	}

	@Test
	@DisplayName("배송 완료는 배송 중인 키트만, 한 번만 바꾼다")
	void completeDeliveryRejected() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));
		int itemId = itemIdOf(paidOrder());

		assertThatThrownBy(() -> kitDeliveryUseCase.completeDelivery(SELLER_ID, itemId, LocalDateTime.now()))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("아직 발송하지 않은 키트입니다.");
		kitDeliveryUseCase.ship(SELLER_ID, itemId, CARRIER, TRACKING_NUMBER, LocalDateTime.now());
		assertThatThrownBy(() -> kitDeliveryUseCase.completeDelivery(OTHER_SELLER_ID, itemId, LocalDateTime.now()))
			.isInstanceOf(OrderAccessDeniedException.class);
		kitDeliveryUseCase.completeDelivery(SELLER_ID, itemId, LocalDateTime.now());
		assertThatThrownBy(() -> kitDeliveryUseCase.completeDelivery(SELLER_ID, itemId, LocalDateTime.now()))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("이미 배송 완료한 키트입니다.");
		assertThatThrownBy(() -> ship(itemId))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("이미 발송한 키트입니다.");
	}

	@Test
	@DisplayName("판매자 키트 목록은 결제한 내 키트만 상태로 걸러 보이고, 건수는 상태별·환불 기한 전으로 센다")
	void sellerKitsAndSummary() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));
		raceCoursePort.put(StubRaceCoursePort.openCourse(OPEN_COURSE_ID, RACE_ID, 14, null,
			LocalDate.now().plusDays(10)));
		int shippingItemId = itemIdOf(paidOrder(MEMBER_ID));
		int preparingOrderId = paidOrder(MEMBER_ID + 1);
		int beforeDeadlineItemId = itemIdOf(paidOrder(MEMBER_ID + 2, OPEN_COURSE_ID));
		createOrder(MEMBER_ID + 3, COURSE_ID);
		createOrder(MEMBER_ID + 4, OPEN_COURSE_ID);
		ship(shippingItemId);

		assertThat(kitDeliveryUseCase.findKits(SELLER_ID, null, null))
			.extracting(SellerKitDto::getOrderItemId)
			.containsExactly(beforeDeadlineItemId, itemIdOf(preparingOrderId), shippingItemId);
		List<SellerKitDto> preparing = kitDeliveryUseCase.findKits(SELLER_ID, RACE_ID, KitDeliveryStatus.PREPARING);
		assertThat(preparing).extracting(SellerKitDto::getOrderItemId)
			.containsExactly(beforeDeadlineItemId, itemIdOf(preparingOrderId));
		SellerKitDto row = preparing.get(1);
		assertThat(row.getOrderNo()).isNotBlank();
		assertThat(row.getParticipantName()).isEqualTo("김러너");
		assertThat(row.getTshirtSize()).isEqualTo("L");
		assertThat(row.getShippingAddress().getAddress()).isEqualTo("서울 성동구 왕십리로 00");
		assertThat(kitDeliveryUseCase.findKits(OTHER_SELLER_ID, null, null)).isEmpty();
		assertThat(kitDeliveryUseCase.findKits(SELLER_ID, RACE_ID + 1, null)).isEmpty();

		SellerKitSummaryDto summary = kitDeliveryUseCase.summarize(SELLER_ID, null, LocalDateTime.now());
		assertThat(summary.getPreparing()).isEqualTo(2);
		assertThat(summary.getShipping()).isEqualTo(1);
		assertThat(summary.getDelivered()).isZero();
		assertThat(summary.getBeforeRefundDeadline()).isEqualTo(1);
		SellerKitSummaryDto otherRace = kitDeliveryUseCase.summarize(SELLER_ID, RACE_ID + 1, LocalDateTime.now());
		assertThat(otherRace.getPreparing() + otherRace.getShipping() + otherRace.getBeforeRefundDeadline()).isZero();
	}

	@Test
	@DisplayName("건수를 셀 때 상품 서비스에서 찾지 못한 종목은 환불 기한 전 건수에서만 빼고 나머지는 센다")
	void summarizeWithMissingCourse() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().plusDays(10)));
		paidOrder();
		raceCoursePort.clear();

		SellerKitSummaryDto summary = kitDeliveryUseCase.summarize(SELLER_ID, null, LocalDateTime.now());

		assertThat(summary.getPreparing()).isEqualTo(1);
		assertThat(summary.getBeforeRefundDeadline()).isZero();
	}

	@Test
	@DisplayName("일정 변경 뒤 7일처럼 환불 기한이 지나도 환불할 수 있는 동안은 발송을 막는다 (취소와 같은 기준)")
	void shipWhileScheduleChangeRefund() throws Exception {
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null, LocalDate.now().minusDays(1),
			LocalDateTime.now().minusDays(1)));
		int itemId = itemIdOf(paidOrder());

		assertThatThrownBy(() -> ship(itemId))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessageStartingWith("아직 환불할 수 있는 참가권이라");
	}

	@Test
	@DisplayName("보낸 뒤 취소된 참가권도 판매자 목록에 취소 상태로 남고, 배송 완료로 바꿀 수 있다")
	void canceledAfterShipping() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));
		int orderId = paidOrder();
		int itemId = itemIdOf(orderId);
		ship(itemId);
		// 일정이 바뀌어 7일 동안 다시 환불할 수 있게 되자 구매자가 취소했다
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null, LocalDate.now().minusDays(1),
			LocalDateTime.now()));
		mockMvc.perform(post("/api/v1/order/orders/" + orderId + "/items/" + itemId + "/cancel")
				.header("X-Member-Id", MEMBER_ID))
			.andExpect(status().isOk());

		assertThat(kitDeliveryUseCase.findKits(SELLER_ID, null, KitDeliveryStatus.SHIPPING))
			.extracting(SellerKitDto::getOrderItemId, SellerKitDto::getOrderItemStatus)
			.containsExactly(tuple(itemId, OrderItemStatus.CANCELED));
		assertThat(kitDeliveryUseCase.summarize(SELLER_ID, null, LocalDateTime.now()).getShipping()).isEqualTo(1);
		kitDeliveryUseCase.completeDelivery(SELLER_ID, itemId, LocalDateTime.now());
		assertThat(itemOf(orderId).getKitDeliveryStatus()).isEqualTo(KitDeliveryStatus.DELIVERED);
	}

	@Test
	@DisplayName("택배사·송장 번호가 비었거나 너무 길면 400으로 막고 배송 준비로 남긴다")
	void shipWithInvalidShippingInfo() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));
		int orderId = paidOrder();
		int itemId = itemIdOf(orderId);

		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, itemId, " ", TRACKING_NUMBER, LocalDateTime.now()))
			.isInstanceOf(KitShippingInfoInvalidException.class);
		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, itemId, CARRIER, null, LocalDateTime.now()))
			.isInstanceOf(KitShippingInfoInvalidException.class);
		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, itemId, CARRIER,
			"1".repeat(OrderItem.MAX_TRACKING_NUMBER_LENGTH + 1), LocalDateTime.now()))
			.isInstanceOf(KitShippingInfoInvalidException.class);
		assertThat(itemOf(orderId).getKitDeliveryStatus()).isEqualTo(KitDeliveryStatus.PREPARING);
	}

	@Test
	@DisplayName("환불 기한 안에는 발송 처리를 막는다 (정책: 키트는 환불 기한이 지난 뒤 배송)")
	void shipBeforeRefundDeadline() throws Exception {
		LocalDate refundDeadline = LocalDate.now();
		raceCoursePort.put(courseWithRefundDeadline(refundDeadline));
		int orderId = paidOrder();

		assertThatThrownBy(() -> ship(itemIdOf(orderId)))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("아직 환불할 수 있는 참가권이라 발송할 수 없습니다(환불 기한: %s).".formatted(refundDeadline));
		assertThat(itemOf(orderId).getKitDeliveryStatus()).isEqualTo(KitDeliveryStatus.PREPARING);
	}

	@Test
	@DisplayName("다른 판매자의 참가권은 403, 없는 주문 상품은 404, 이미 발송한 키트는 409다")
	void shipRejected() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));
		int itemId = itemIdOf(paidOrder());

		assertThatThrownBy(() -> kitDeliveryUseCase.ship(OTHER_SELLER_ID, itemId, CARRIER, TRACKING_NUMBER,
			LocalDateTime.now()))
			.isInstanceOf(OrderAccessDeniedException.class);
		assertThatThrownBy(() -> ship(999_999))
			.isInstanceOf(OrderItemNotFoundException.class);

		ship(itemId);
		assertThatThrownBy(() -> ship(itemId))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("이미 발송한 키트입니다.");
	}

	@Test
	@DisplayName("결제 전이거나 취소한 참가권의 키트는 발송하지 않는다")
	void shipUnpaidOrCanceled() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().plusDays(10)));
		int waitingOrderId = createOrder(MEMBER_ID);
		int canceledOrderId = paidOrder(MEMBER_ID + 1);
		int canceledItemId = itemIdOf(canceledOrderId);
		mockMvc.perform(post("/api/v1/order/orders/" + canceledOrderId + "/items/" + canceledItemId + "/cancel")
				.header("X-Member-Id", MEMBER_ID + 1))
			.andExpect(status().isOk());
		// 환불 기한이 지난 뒤에도 결제 상태를 따로 확인한다
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));

		assertThatThrownBy(() -> ship(itemIdOf(waitingOrderId)))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("결제한 참가권의 키트만 발송할 수 있습니다(현재 상태: 결제 대기).");
		assertThatThrownBy(() -> ship(canceledItemId))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("결제한 참가권의 키트만 발송할 수 있습니다(현재 상태: 취소).");
	}

	@Test
	@DisplayName("키트가 없는 종목은 배송 상태가 없고 발송 처리할 수 없다")
	void courseWithoutKit() throws Exception {
		raceCoursePort.put(StubRaceCoursePort.courseWithoutKit(COURSE_ID, RACE_ID));
		int orderId = paidOrder();

		findOrder(orderId).andExpect(jsonPath("$.data.items[0].kitDeliveryStatus").isEmpty());
		assertThatThrownBy(() -> ship(itemIdOf(orderId)))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("키트가 없는 참가권입니다.");
	}

	private RaceCourseInfo courseWithRefundDeadline(LocalDate refundDeadline) {
		return StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null, refundDeadline);
	}

	private OrderItemDto ship(int orderItemId) {
		return kitDeliveryUseCase.ship(SELLER_ID, orderItemId, CARRIER, TRACKING_NUMBER, LocalDateTime.now());
	}

	private ResultActions findOrder(int orderId) throws Exception {
		return mockMvc.perform(get("/api/v1/order/orders/" + orderId).header("X-Member-Id", MEMBER_ID))
			.andExpect(status().isOk());
	}

	private int paidOrder() throws Exception {
		return paidOrder(MEMBER_ID);
	}

	private int paidOrder(int memberId) throws Exception {
		return paidOrder(memberId, COURSE_ID);
	}

	private int paidOrder(int memberId, int courseId) throws Exception {
		int orderId = createOrder(memberId, courseId);
		transactionTemplate.execute(status -> orderPayUseCase.pay(memberId, orderId, LocalDateTime.now()));
		return orderId;
	}

	private int createOrder(int memberId) throws Exception {
		return createOrder(memberId, COURSE_ID);
	}

	private int createOrder(int memberId, int courseId) throws Exception {
		int participationId = apply(memberId, courseId);
		OrderCreateResult result = transactionTemplate.execute(status -> orderCreateUseCase.create(memberId,
			participationId, UUID.randomUUID().toString(), LocalDateTime.now()));
		return result.order().getId();
	}

	private int apply(int memberId, int courseId) throws Exception {
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
					""".formatted(courseId)))
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.id");
	}

	private OrderItem itemOf(int orderId) {
		return transactionTemplate.execute(status -> {
			Order order = orderRepository.findById(orderId).orElseThrow();
			return order.getItems().getFirst();
		});
	}

	private int itemIdOf(int orderId) {
		return itemOf(orderId).getId();
	}
}
