package com.bukang.order.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.bukang.order.domain.ParticipationStatus;
import com.bukang.order.out.CartItemRepository;
import com.bukang.order.out.CartRepository;
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
class ApiV1CartCheckoutTest {
	private static final String CHECKOUT_URL = "/api/v1/order/cart/checkout";
	private static final int MEMBER_ID = 1;
	private static final int OTHER_MEMBER_ID = 2;
	private static final int COURSE_ID = 10;
	private static final int OTHER_COURSE_ID = 11;
	private static final int THIRD_COURSE_ID = 12;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CartRepository cartRepository;

	// 장바구니 빼기만 일부러 실패시킬 때 쓴다. 그 밖에는 진짜 저장소처럼 동작한다
	@MockitoSpyBean
	private CartItemRepository cartItemRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ParticipationRepository participationRepository;

	@Autowired
	private StubRaceCoursePort raceCoursePort;

	@Autowired
	private StubSeatHoldPort seatHoldPort;

	@BeforeEach
	void setUp() {
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID, 100, 14, null));
		raceCoursePort.put(StubRaceCoursePort.openCourse(OTHER_COURSE_ID, 101, 14, null));
		raceCoursePort.put(StubRaceCoursePort.openCourse(THIRD_COURSE_ID, 102, 14, null));
	}

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
		participationRepository.deleteAll();
		cartRepository.deleteAll();
		raceCoursePort.clear();
		seatHoldPort.clear();
	}

	@Test
	@DisplayName("장바구니의 참가권 두 장을 한 번에 결제하면 참가권마다 주문이 1건씩 생기고, 주문한 항목은 장바구니에서 빠진다")
	void checkout() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		addToCart(MEMBER_ID, OTHER_COURSE_ID);
		int first = apply(MEMBER_ID, COURSE_ID);
		int second = apply(MEMBER_ID, OTHER_COURSE_ID);

		checkout(MEMBER_ID, newKey(), first, second)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("참가권 2건 중 2건의 자리를 확보했습니다. 결제 기한 안에 결제해주세요."))
			.andExpect(jsonPath("$.data.orderedCount").value(2))
			.andExpect(jsonPath("$.data.results[0].participationId").value(first))
			.andExpect(jsonPath("$.data.results[0].outcome").value("CREATED"))
			.andExpect(jsonPath("$.data.results[0].order.status").value("CREATED"))
			.andExpect(jsonPath("$.data.results[0].order.items[0].participationId").value(first))
			.andExpect(jsonPath("$.data.results[1].participationId").value(second))
			.andExpect(jsonPath("$.data.results[1].outcome").value("CREATED"))
			.andExpect(jsonPath("$.data.results[1].order.items[0].participationId").value(second));

		assertThat(orderRepository.count()).isEqualTo(2);
		assertThat(statusOf(first)).isEqualTo(ParticipationStatus.APPLIED);
		assertThat(statusOf(second)).isEqualTo(ParticipationStatus.APPLIED);
		assertThat(cartCourseIds(MEMBER_ID)).isEmpty();
	}

	@Test
	@DisplayName("일부만 매진이면 나머지는 주문하고, 매진된 참가 신청은 FAILED가 되며 그 항목은 장바구니에 남는다")
	void checkoutPartlySoldOut() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		addToCart(MEMBER_ID, OTHER_COURSE_ID);
		int available = apply(MEMBER_ID, COURSE_ID);
		int soldOut = apply(MEMBER_ID, OTHER_COURSE_ID);
		seatHoldPort.setRemainingSeats(OTHER_COURSE_ID, 0);

		checkout(MEMBER_ID, newKey(), available, soldOut)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("참가권 2건 중 1건의 자리를 확보했습니다. 결제 기한 안에 결제해주세요."))
			.andExpect(jsonPath("$.data.orderedCount").value(1))
			.andExpect(jsonPath("$.data.results[0].outcome").value("CREATED"))
			.andExpect(jsonPath("$.data.results[1].outcome").value("SOLD_OUT"))
			.andExpect(jsonPath("$.data.results[1].message").value("남은 자리가 없습니다. 참가 신청서를 다시 작성해 신청할 수 있습니다."))
			.andExpect(jsonPath("$.data.results[1].order").doesNotExist());

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(statusOf(available)).isEqualTo(ParticipationStatus.APPLIED);
		assertThat(statusOf(soldOut)).isEqualTo(ParticipationStatus.FAILED);
		assertThat(cartCourseIds(MEMBER_ID)).containsExactly(OTHER_COURSE_ID);
	}

	@Test
	@DisplayName("결제할 수 없는 항목(접수 마감)은 FAILED로 알려 주고 장바구니에 남기며, 나머지는 그대로 주문한다")
	void checkoutWithFailedItem() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		addToCart(MEMBER_ID, OTHER_COURSE_ID);
		int closed = apply(MEMBER_ID, COURSE_ID);
		int open = apply(MEMBER_ID, OTHER_COURSE_ID);
		// 신청서를 쓴 뒤 접수가 마감됐다
		raceCoursePort.put(StubRaceCoursePort.closedCourse(COURSE_ID, 100));

		checkout(MEMBER_ID, newKey(), closed, open)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderedCount").value(1))
			.andExpect(jsonPath("$.data.results[0].outcome").value("FAILED"))
			.andExpect(jsonPath("$.data.results[0].message").value("접수 기간이 아닙니다."))
			.andExpect(jsonPath("$.data.results[1].outcome").value("CREATED"));

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(statusOf(closed)).isEqualTo(ParticipationStatus.PENDING);
		assertThat(cartCourseIds(MEMBER_ID)).containsExactly(COURSE_ID);
	}

	@Test
	@DisplayName("한 항목에서 예상하지 못한 오류가 나도 500이 아니라 그 항목만 FAILED이고, 앞에서 만든 주문은 남는다")
	void checkoutWithUnexpectedError() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		addToCart(MEMBER_ID, OTHER_COURSE_ID);
		int first = apply(MEMBER_ID, COURSE_ID);
		int broken = apply(MEMBER_ID, OTHER_COURSE_ID);
		seatHoldPort.failHold(OTHER_COURSE_ID);

		checkout(MEMBER_ID, newKey(), first, broken)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderedCount").value(1))
			.andExpect(jsonPath("$.data.results[0].outcome").value("CREATED"))
			.andExpect(jsonPath("$.data.results[1].outcome").value("FAILED"))
			.andExpect(jsonPath("$.data.results[1].message").value("처리하지 못했습니다. 같은 Idempotency-Key로 다시 시도해주세요."));

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(statusOf(broken)).isEqualTo(ParticipationStatus.PENDING);
		assertThat(cartCourseIds(MEMBER_ID)).containsExactly(OTHER_COURSE_ID);
	}

	@Test
	@DisplayName("주문한 종목을 장바구니에서 빼지 못해도 주문은 그대로 남고, 그 항목은 장바구니에 남는다")
	void checkoutWhenCartRemovalFails() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		int participationId = apply(MEMBER_ID, COURSE_ID);
		doThrow(new IllegalStateException("장바구니 잠금 대기 시간 초과 (테스트)"))
			.when(cartItemRepository).deleteByCartIdAndRaceCourseId(anyInt(), anyInt());

		checkout(MEMBER_ID, newKey(), participationId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderedCount").value(1))
			.andExpect(jsonPath("$.data.results[0].outcome").value("CREATED"));

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(statusOf(participationId)).isEqualTo(ParticipationStatus.APPLIED);
		assertThat(cartCourseIds(MEMBER_ID)).containsExactly(COURSE_ID);
	}

	@Test
	@DisplayName("같은 Idempotency-Key로 동시에 두 번 보내도 참가권마다 주문은 1건이고 자리도 한 번만 잡는다")
	void checkoutTwiceTogether() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		addToCart(MEMBER_ID, OTHER_COURSE_ID);
		int first = apply(MEMBER_ID, COURSE_ID);
		int second = apply(MEMBER_ID, OTHER_COURSE_ID);
		String key = newKey();

		CountDownLatch start = new CountDownLatch(1);
		Callable<String> request = () -> {
			start.await();
			return checkout(MEMBER_ID, key, first, second).andReturn().getResponse().getContentAsString();
		};
		List<String> bodies = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			List<Future<String>> futures = List.of(executor.submit(request), executor.submit(request));
			start.countDown();
			for (Future<String> future : futures) {
				bodies.add(future.get(30, TimeUnit.SECONDS));
			}
		}

		for (String body : bodies) {
			assertThat((Integer)JsonPath.read(body, "$.status")).isEqualTo(200);
			assertThat((Integer)JsonPath.read(body, "$.data.orderedCount")).isEqualTo(2);
		}
		assertThat(orderRepository.count()).isEqualTo(2);
		assertThat(seatHoldPort.holdCalls()).isEqualTo(2);
		assertThat(cartCourseIds(MEMBER_ID)).isEmpty();
	}

	@Test
	@DisplayName("같은 Idempotency-Key로 다시 보내면 새 주문·자리 확보 없이 처음 주문을 EXISTING으로 돌려준다")
	void checkoutRetryWithSameKey() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		addToCart(MEMBER_ID, OTHER_COURSE_ID);
		int first = apply(MEMBER_ID, COURSE_ID);
		int second = apply(MEMBER_ID, OTHER_COURSE_ID);
		String key = newKey();
		int firstOrderId = JsonPath.read(checkout(MEMBER_ID, key, first, second)
			.andReturn().getResponse().getContentAsString(), "$.data.results[0].order.id");

		checkout(MEMBER_ID, key, first, second)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderedCount").value(2))
			.andExpect(jsonPath("$.data.results[0].outcome").value("EXISTING"))
			.andExpect(jsonPath("$.data.results[0].message").value("이미 처리한 주문 요청입니다."))
			.andExpect(jsonPath("$.data.results[0].order.id").value(firstOrderId))
			.andExpect(jsonPath("$.data.results[1].outcome").value("EXISTING"));

		assertThat(orderRepository.count()).isEqualTo(2);
		assertThat(seatHoldPort.holdCalls()).isEqualTo(2);
	}

	@Test
	@DisplayName("다른 키로 이미 주문한 신청서를 다시 보내면 그 항목만 FAILED이고 자리는 다시 잡지 않는다")
	void checkoutAlreadyOrderedWithOtherKey() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		int participationId = apply(MEMBER_ID, COURSE_ID);
		checkout(MEMBER_ID, newKey(), participationId).andExpect(status().isOk());

		checkout(MEMBER_ID, newKey(), participationId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("주문한 참가권이 없습니다. 항목별 결과를 확인해주세요."))
			.andExpect(jsonPath("$.data.orderedCount").value(0))
			.andExpect(jsonPath("$.data.results[0].outcome").value("FAILED"));

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(seatHoldPort.holdCalls()).isEqualTo(1);
	}

	@Test
	@DisplayName("장바구니에서 이미 뺀 종목이어도 주문은 그대로 만들고, 다른 회원의 장바구니는 건드리지 않는다")
	void checkoutItemNotInCart() throws Exception {
		addToCart(OTHER_MEMBER_ID, COURSE_ID);
		int participationId = apply(MEMBER_ID, COURSE_ID);

		checkout(MEMBER_ID, newKey(), participationId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results[0].outcome").value("CREATED"));

		assertThat(cartCourseIds(OTHER_MEMBER_ID)).containsExactly(COURSE_ID);
	}

	@Test
	@DisplayName("다른 회원의 참가 신청이 섞여 있으면 403, 없는 참가 신청이 섞여 있으면 404이고 자리를 하나도 잡지 않는다")
	void checkoutNotOwnerOrMissing() throws Exception {
		addToCart(MEMBER_ID, COURSE_ID);
		int mine = apply(MEMBER_ID, COURSE_ID);
		int others = apply(OTHER_MEMBER_ID, OTHER_COURSE_ID);

		checkout(MEMBER_ID, newKey(), mine, others)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("본인의 참가 신청만 결제할 수 있습니다."));
		checkout(MEMBER_ID, newKey(), mine, 999999)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 참가 신청입니다."));

		assertThat(seatHoldPort.holdCalls()).isZero();
		assertThat(orderRepository.count()).isZero();
		assertThat(cartCourseIds(MEMBER_ID)).containsExactly(COURSE_ID);
	}

	@Test
	@DisplayName("참가 신청이 없거나, 10건을 넘거나, 같은 신청서가 두 번이거나, ID가 0 이하면 400이고 자리를 잡지 않는다")
	void checkoutInvalidRequest() throws Exception {
		int participationId = apply(MEMBER_ID, COURSE_ID);
		Integer[] eleven = IntStream.rangeClosed(1, 11).boxed().toArray(Integer[]::new);

		checkout(MEMBER_ID, newKey())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("결제할 참가 신청을 선택해주세요."));
		checkout(MEMBER_ID, newKey(), eleven)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("한 번에 10건까지 결제할 수 있습니다."));
		checkout(MEMBER_ID, newKey(), participationId, participationId)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("같은 참가 신청을 두 번 보낼 수 없습니다."));
		checkout(MEMBER_ID, newKey(), 0)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("참가 신청 ID가 올바르지 않습니다."));

		assertThat(seatHoldPort.holdCalls()).isZero();
	}

	@Test
	@DisplayName("Idempotency-Key가 없으면 400, 64자를 넘으면 400이다")
	void checkoutInvalidIdempotencyKey() throws Exception {
		int participationId = apply(MEMBER_ID, COURSE_ID);

		mockMvc.perform(post(CHECKOUT_URL)
				.header("X-Member-Id", MEMBER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(participationId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("필수 요청 헤더(Idempotency-Key)가 없습니다."));
		checkout(MEMBER_ID, "k".repeat(65), participationId)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Idempotency-Key는 1~64자로 보내주세요."));

		assertThat(seatHoldPort.holdCalls()).isZero();
	}

	private ResultActions checkout(int memberId, String key, Integer... participationIds) throws Exception {
		return mockMvc.perform(post(CHECKOUT_URL)
			.header("X-Member-Id", memberId)
			.header("Idempotency-Key", key)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(participationIds)));
	}

	private String body(Integer... participationIds) {
		String ids = List.of(participationIds).stream().map(String::valueOf).collect(Collectors.joining(", "));
		return "{\"participationIds\": [%s]}".formatted(ids);
	}

	private void addToCart(int memberId, int raceCourseId) throws Exception {
		mockMvc.perform(post("/api/v1/order/cart/items")
				.header("X-Member-Id", memberId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"raceCourseId\": %d}".formatted(raceCourseId)))
			.andExpect(status().isCreated());
	}

	private int apply(int memberId, int raceCourseId) throws Exception {
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

	private ParticipationStatus statusOf(int participationId) {
		return participationRepository.findById(participationId).orElseThrow().getStatus();
	}

	private List<Integer> cartCourseIds(int memberId) throws Exception {
		String body = mockMvc.perform(get("/api/v1/order/cart").header("X-Member-Id", memberId))
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.items[*].raceCourseId");
	}

	private String newKey() {
		return UUID.randomUUID().toString();
	}
}
