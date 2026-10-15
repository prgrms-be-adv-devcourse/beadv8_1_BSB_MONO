package com.bukang.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.bukang.order.domain.exception.KitNotShippableException;
import com.bukang.order.domain.exception.OrderAccessDeniedException;
import com.bukang.order.domain.exception.OrderItemNotFoundException;
import com.bukang.order.dto.OrderItemDto;
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
	@DisplayName("결제한 키트는 배송 준비로 보이고, 환불 기한이 지나면 판매자가 발송 처리해 구매자 주문 조회에 발송으로 보인다")
	void shipAfterRefundDeadline() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));
		int orderId = paidOrder();
		int itemId = itemIdOf(orderId);
		findOrder(orderId)
			.andExpect(jsonPath("$.data.items[0].kitDeliveryStatus").value("PREPARING"))
			.andExpect(jsonPath("$.data.items[0].kitShippedAt").isEmpty());

		LocalDateTime now = LocalDateTime.now();
		OrderItemDto shipped = kitDeliveryUseCase.ship(SELLER_ID, itemId, now);

		assertThat(shipped.getKitDeliveryStatus()).isEqualTo(KitDeliveryStatus.SHIPPED);
		findOrder(orderId)
			.andExpect(jsonPath("$.data.items[0].kitDeliveryStatus").value("SHIPPED"))
			.andExpect(jsonPath("$.data.items[0].kitShippedAt").isNotEmpty());
		assertThat(itemOf(orderId).getKitShippedAt()).isEqualTo(now);
	}

	@Test
	@DisplayName("환불 기한 안에는 발송 처리를 막는다 (정책: 키트는 환불 기한이 지난 뒤 배송)")
	void shipBeforeRefundDeadline() throws Exception {
		LocalDate refundDeadline = LocalDate.now();
		raceCoursePort.put(courseWithRefundDeadline(refundDeadline));
		int orderId = paidOrder();

		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, itemIdOf(orderId), LocalDateTime.now()))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("환불 기한(%s)이 지난 뒤 발송할 수 있습니다.".formatted(refundDeadline));
		assertThat(itemOf(orderId).getKitDeliveryStatus()).isEqualTo(KitDeliveryStatus.PREPARING);
	}

	@Test
	@DisplayName("다른 판매자의 참가권은 403, 없는 주문 상품은 404, 이미 발송한 키트는 409다")
	void shipRejected() throws Exception {
		raceCoursePort.put(courseWithRefundDeadline(LocalDate.now().minusDays(1)));
		int itemId = itemIdOf(paidOrder());

		assertThatThrownBy(() -> kitDeliveryUseCase.ship(OTHER_SELLER_ID, itemId, LocalDateTime.now()))
			.isInstanceOf(OrderAccessDeniedException.class);
		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, 999_999, LocalDateTime.now()))
			.isInstanceOf(OrderItemNotFoundException.class);

		kitDeliveryUseCase.ship(SELLER_ID, itemId, LocalDateTime.now());
		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, itemId, LocalDateTime.now()))
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

		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, itemIdOf(waitingOrderId), LocalDateTime.now()))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("결제한 참가권의 키트만 발송할 수 있습니다(현재 상태: 결제 대기).");
		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, canceledItemId, LocalDateTime.now()))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("결제한 참가권의 키트만 발송할 수 있습니다(현재 상태: 취소).");
	}

	@Test
	@DisplayName("키트가 없는 종목은 배송 상태가 없고 발송 처리할 수 없다")
	void courseWithoutKit() throws Exception {
		raceCoursePort.put(StubRaceCoursePort.courseWithoutKit(COURSE_ID, RACE_ID));
		int orderId = paidOrder();

		findOrder(orderId).andExpect(jsonPath("$.data.items[0].kitDeliveryStatus").isEmpty());
		assertThatThrownBy(() -> kitDeliveryUseCase.ship(SELLER_ID, itemIdOf(orderId), LocalDateTime.now()))
			.isInstanceOf(KitNotShippableException.class)
			.hasMessage("키트가 없는 참가권입니다.");
	}

	private RaceCourseInfo courseWithRefundDeadline(LocalDate refundDeadline) {
		return StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null, refundDeadline);
	}

	private ResultActions findOrder(int orderId) throws Exception {
		return mockMvc.perform(get("/api/v1/order/orders/" + orderId).header("X-Member-Id", MEMBER_ID))
			.andExpect(status().isOk());
	}

	private int paidOrder() throws Exception {
		return paidOrder(MEMBER_ID);
	}

	private int paidOrder(int memberId) throws Exception {
		int orderId = createOrder(memberId);
		transactionTemplate.execute(status -> orderPayUseCase.pay(memberId, orderId, LocalDateTime.now()));
		return orderId;
	}

	private int createOrder(int memberId) throws Exception {
		int participationId = apply(memberId);
		OrderCreateResult result = transactionTemplate.execute(status -> orderCreateUseCase.create(memberId,
			participationId, UUID.randomUUID().toString(), LocalDateTime.now()));
		return result.order().getId();
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
