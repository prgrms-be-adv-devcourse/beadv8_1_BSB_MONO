package com.bukang.product.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.common.shared.seller.domain.SellerType;
import com.bukang.product.TestcontainersConfiguration;
import com.bukang.product.app.ProductFacade;
import com.bukang.product.domain.ProductMember;
import com.bukang.product.domain.ProductSeller;
import com.bukang.product.dto.RaceSaveRequestDto;
import com.bukang.product.out.ParticipationHoldRepository;
import com.bukang.product.out.ProductMemberRepository;
import com.bukang.product.out.ProductSellerRepository;
import com.bukang.product.out.RaceCourseDocumentRepository;
import com.bukang.product.out.RaceCourseRepository;
import com.bukang.product.out.RaceRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * product-service 통합 테스트의 공통 설정. 모든 테스트 클래스가 이 클래스를 상속해 같은 설정을 쓰므로,
 * Spring 컨텍스트와 ES 컨테이너(nori)를 한 번만 띄워 함께 쓴다.
 * DB는 H2(test 프로파일)이고, ES 컨테이너가 필요해 Docker가 없으면 건너뛴다.
 * Kafka는 브로커 없이 보낸 메시지만 확인한다 (send()는 바로 성공한 것으로 돌려준다).
 * 테스트마다 승인된 판매자 2명(회원 9001 → 판매자 7001, 회원 9002 → 판매자 7002)을 넣고, 끝나면 DB와 검색 문서를 비운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
public abstract class ProductIntegrationTest {
	protected static final int SELLER_MEMBER_ID = 9001;
	protected static final int SELLER_ID = 7001;
	protected static final int OTHER_SELLER_MEMBER_ID = 9002;
	protected static final int OTHER_SELLER_ID = 7002;
	// 판매자가 아닌 회원 (복제본도 없다)
	protected static final int NOT_SELLER_MEMBER_ID = 9999;

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected JsonConverter jsonConverter;
	@Autowired
	protected TransactionTemplate transactionTemplate;
	@Autowired
	protected ProductFacade productFacade;
	@Autowired
	protected ProductMemberRepository productMemberRepository;
	@Autowired
	protected ProductSellerRepository productSellerRepository;
	@Autowired
	protected RaceRepository raceRepository;
	@Autowired
	protected RaceCourseRepository raceCourseRepository;
	@Autowired
	protected ParticipationHoldRepository participationHoldRepository;
	@Autowired
	protected RaceCourseDocumentRepository raceCourseDocumentRepository;

	// 실제 KafkaTemplate을 감싼다. 자동 설정 빈의 타입이 KafkaTemplate<?, ?>라 같은 타입으로 받는다
	@MockitoSpyBean
	private KafkaTemplate<?, ?> kafkaTemplate;

	// 등록 결과: 대회 ID와 요청 순서대로의 종목 ID
	protected record RegisteredRace(int raceId, List<Integer> courseIds) {
		public int courseId() {
			return courseIds.getFirst();
		}
	}

	@BeforeEach
	void setUpProductTest() {
		// 브로커 없이 보낸 것으로 처리한다 (보내지 않으면 send()가 브로커를 기다리다 max.block.ms 뒤 실패한다)
		doReturn(CompletableFuture.completedFuture(null)).when(kafka()).send(anyString(), anyString(), anyString());
		seedSeller(SELLER_MEMBER_ID, SELLER_ID);
		seedSeller(OTHER_SELLER_MEMBER_ID, OTHER_SELLER_ID);
	}

	// FK 순서대로 지운다: 자리 → 대회(종목은 함께 지워진다) → 판매자 → 회원, 그리고 검색 문서
	@AfterEach
	void cleanUpProductTest() {
		participationHoldRepository.deleteAll();
		raceRepository.deleteAll();
		productSellerRepository.deleteAll();
		productMemberRepository.deleteAll();
		raceCourseDocumentRepository.deleteAll();
	}

	@SuppressWarnings("unchecked")
	protected KafkaTemplate<String, String> kafka() {
		return (KafkaTemplate<String, String>)kafkaTemplate;
	}

	// 이 테스트에서 토픽으로 보낸 메시지 본문 (보낸 순서대로)
	protected List<String> sentMessages(String topic) {
		ArgumentCaptor<String> payloads = ArgumentCaptor.forClass(String.class);
		verify(kafka(), atLeast(0)).send(eq(topic), anyString(), payloads.capture());
		return payloads.getAllValues();
	}

	protected void seedSeller(int memberId, int sellerId) {
		LocalDateTime now = LocalDateTime.now();
		ProductMember member = productMemberRepository.save(new ProductMember(memberId, now, now, "러너" + memberId));
		productSellerRepository.save(new ProductSeller(sellerId, member, SellerType.BUSINESS, "(주)러닝" + sellerId,
			"123-45-67890", "김대표", SellerStatus.APPROVED, now, now));
	}

	// ---------- 대회 등록·판매 시작 ----------

	protected RegisteredRace register(RaceSaveRequestDto request) throws Exception {
		return register(SELLER_MEMBER_ID, request);
	}

	protected RegisteredRace register(int memberId, RaceSaveRequestDto request) throws Exception {
		MvcResult result = postJson("/api/v1/product/races", memberId, request)
			.andExpect(status().isCreated())
			.andReturn();
		String body = result.getResponse().getContentAsString();
		int raceId = JsonPath.read(body, "$.data.raceId");
		List<Integer> courseIds = JsonPath.read(body, "$.data.courseIds");
		return new RegisteredRace(raceId, courseIds);
	}

	protected void startSale(int raceId) throws Exception {
		postAs("/api/v1/product/races/" + raceId + "/sale", SELLER_MEMBER_ID).andExpect(status().isOk());
	}

	// 등록하고 판매 시작까지 누른다 (기본 요청이면 개인 예매가 이미 열려 판매 중이 된다)
	protected RegisteredRace registerOnSale(RaceSaveRequestDto request) throws Exception {
		RegisteredRace race = register(request);
		startSale(race.raceId());
		return race;
	}

	// ---------- 자리 (주문 서비스가 부르는 내부 API) ----------

	protected ResultActions hold(int raceCourseId, int participationId, LocalDateTime expiresAt) throws Exception {
		String body = "{\"raceCourseId\":%d,\"participationId\":%d,\"expiresAt\":\"%s\"}"
			.formatted(raceCourseId, participationId, expiresAt.withNano(0));
		return mockMvc.perform(post("/internal/v1/product/holds")
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	protected ResultActions hold(int raceCourseId, int participationId) throws Exception {
		return hold(raceCourseId, participationId, LocalDateTime.now().plusMinutes(10));
	}

	protected ResultActions sell(int participationId) throws Exception {
		return mockMvc.perform(post("/internal/v1/product/holds/" + participationId + "/sell"));
	}

	protected ResultActions restore(int participationId) throws Exception {
		return mockMvc.perform(post("/internal/v1/product/holds/" + participationId + "/restore"));
	}

	// ---------- 요청 ----------

	// memberId가 null이면 X-Member-Id 헤더를 보내지 않는다
	protected ResultActions postJson(String path, Integer memberId, Object body) throws Exception {
		return mockMvc.perform(withMember(post(path), memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(jsonConverter.toJson(body)));
	}

	protected ResultActions putJson(String path, Integer memberId, Object body) throws Exception {
		return mockMvc.perform(withMember(put(path), memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(jsonConverter.toJson(body)));
	}

	protected ResultActions postAs(String path, Integer memberId) throws Exception {
		return mockMvc.perform(withMember(post(path), memberId));
	}

	protected ResultActions getAs(String path, Integer memberId) throws Exception {
		return mockMvc.perform(withMember(get(path), memberId));
	}

	private MockHttpServletRequestBuilder withMember(MockHttpServletRequestBuilder request, Integer memberId) {
		return memberId == null ? request : request.header("X-Member-Id", memberId);
	}

	// ---------- 동시 요청 ----------

	public interface Call<T> {
		T call(int index) throws Exception;
	}

	// count개 요청을 스레드마다 하나씩 띄워 한꺼번에 출발시키고, 결과를 요청 순서대로 돌려준다
	protected <T> List<T> runConcurrently(int count, Call<T> call) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(count);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<T>> futures = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				int index = i;
				futures.add(pool.submit(() -> {
					start.await();
					return call.call(index);
				}));
			}
			start.countDown();
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get());
			}
			return results;
		} finally {
			pool.shutdownNow();
		}
	}
}
