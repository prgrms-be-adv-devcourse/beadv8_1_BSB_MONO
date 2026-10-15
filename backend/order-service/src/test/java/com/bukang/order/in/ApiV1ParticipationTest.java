package com.bukang.order.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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

import com.bukang.order.domain.ParticipantInfo;
import com.bukang.order.domain.Participation;
import com.bukang.order.domain.ParticipationStatus;
import com.bukang.order.out.ParticipationRepository;
import com.bukang.order.support.StubRaceCoursePort;
import com.bukang.order.support.StubRaceCoursePortConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StubRaceCoursePortConfig.class)
class ApiV1ParticipationTest {
	private static final String BASE_URL = "/api/v1/order/participations";
	private static final int MEMBER_ID = 1;
	private static final int OTHER_MEMBER_ID = 2;
	private static final int COURSE_ID = 10;
	private static final int RACE_ID = 100;
	private static final String ADDRESS = "서울 성동구 왕십리로 00";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ParticipationRepository participationRepository;

	@Autowired
	private StubRaceCoursePort raceCoursePort;

	@BeforeEach
	void setUp() {
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null));
	}

	@AfterEach
	void tearDown() {
		participationRepository.deleteAll();
		raceCoursePort.clear();
	}

	@Test
	@DisplayName("신청서를 저장하면 자리 확보 전(PENDING) 개인 신청이 되고 동의 일시가 남는다")
	void apply() throws Exception {
		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.status").value(201))
			.andExpect(jsonPath("$.message").value("참가 신청서를 저장했습니다."))
			.andExpect(jsonPath("$.data.raceId").value(RACE_ID))
			.andExpect(jsonPath("$.data.raceCourseId").value(COURSE_ID))
			.andExpect(jsonPath("$.data.memberId").value(MEMBER_ID))
			.andExpect(jsonPath("$.data.participationType").value("INDIVIDUAL"))
			.andExpect(jsonPath("$.data.status").value("PENDING"))
			.andExpect(jsonPath("$.data.participant.name").value("김러너"))
			.andExpect(jsonPath("$.data.participant.emergencyContactName").value("김가족"))
			.andExpect(jsonPath("$.data.thirdPartyConsent").value(true))
			.andExpect(jsonPath("$.data.consentedAt").isNotEmpty())
			.andExpect(jsonPath("$.data.paymentDueAt").isEmpty());

		Participation saved = participationRepository.findAll().getFirst();
		assertThat(saved.getStatus()).isEqualTo(ParticipationStatus.PENDING);
		assertThat(saved.getActiveFlag()).isTrue();
		assertThat(saved.getCrewApplicationId()).isNull();
	}

	@Test
	@DisplayName("휴대폰 번호는 010-1234-5678 형식으로, 빈 티셔츠 사이즈·상세 주소는 null로, 배송지는 앞뒤 공백을 빼고 저장한다")
	void applyNormalizesInput() throws Exception {
		String body = """
			{ "raceCourseId": %d, "thirdPartyConsent": true,
				"participant": { "name": " 김러너 ", "birth": "1994-06-25", "gender": "FEMALE",
				"phone": "01012345678", "emergencyContactName": "김가족", "emergencyContactPhone": "011-987-6543",
				"tshirtSize": " ", "shippingAddress": { "recipientName": " 김러너 ", "recipientPhone": "01012345678",
				"zipcode": "04700", "address": "  서울 성동구 왕십리로 00  ", "addressDetail": " " } } }
			""".formatted(COURSE_ID);

		apply(MEMBER_ID, body)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.participant.name").value("김러너"))
			.andExpect(jsonPath("$.data.participant.phone").value("010-1234-5678"))
			.andExpect(jsonPath("$.data.participant.emergencyContactPhone").value("011-987-6543"))
			.andExpect(jsonPath("$.data.participant.tshirtSize").isEmpty())
			.andExpect(jsonPath("$.data.participant.shippingAddress.recipientName").value("김러너"))
			.andExpect(jsonPath("$.data.participant.shippingAddress.recipientPhone").value("010-1234-5678"))
			.andExpect(jsonPath("$.data.participant.shippingAddress.zipcode").value("04700"))
			.andExpect(jsonPath("$.data.participant.shippingAddress.address").value("서울 성동구 왕십리로 00"))
			.andExpect(jsonPath("$.data.participant.shippingAddress.addressDetail").isEmpty());
	}

	@Test
	@DisplayName("키트가 있는 종목은 배송지 없이 신청하거나, 주소가 비었거나 255자를 넘거나, 우편번호가 5자리 숫자가 아니면 400이다")
	void applyWithoutShippingAddress() throws Exception {
		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L", null), true))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("키트를 받을 배송 주소를 작성해주세요."))
			.andExpect(jsonPath("$.code").value("ORDER_SHIPPING_ADDRESS_REQUIRED"));
		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L", " "), true))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("주소를 작성해주세요."));
		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L", "가".repeat(256)), true))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("주소는 255자 이하로 작성해주세요."));
		String wrongZipcode = participant("김러너", "1994-06-25", "L", ADDRESS).replace("\"04700\"", "\"0470\"");
		apply(MEMBER_ID, applyBody(COURSE_ID, wrongZipcode, true))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("우편번호는 숫자 5자리로 작성해주세요."));

		assertThat(participationRepository.count()).isZero();
	}

	@Test
	@DisplayName("키트가 없는 종목은 배송지 없이 신청되고, 배송지를 보내도 저장하지 않는다")
	void applyCourseWithoutKit() throws Exception {
		raceCoursePort.put(StubRaceCoursePort.courseWithoutKit(COURSE_ID, RACE_ID));

		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "", null), true))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.participant.shippingAddress").isEmpty());

		apply(OTHER_MEMBER_ID, applyBody(COURSE_ID, participant("박러너", "1990-01-01", "", "서울 어딘가"), true))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.participant.shippingAddress").isEmpty());
		assertThat(participationRepository.findAll())
			.allSatisfy(saved -> assertThat(saved.getParticipant().getShippingAddress()).isNull());
	}

	@Test
	@DisplayName("필수 항목(참가자 정보, 비상 연락처, 제3자 제공 동의)이 빠지면 400이고 저장되지 않는다")
	void applyWithoutRequired() throws Exception {
		apply(MEMBER_ID, applyBody(COURSE_ID, participant("", "1994-06-25", "L"), true))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("참가자 이름을 작성해주세요."))
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		String noEmergency = """
			{ "raceCourseId": %d, "thirdPartyConsent": true,
				"participant": { "name": "김러너", "birth": "1994-06-25", "gender": "MALE",
				"phone": "010-1234-5678", "emergencyContactPhone": "010-9876-5432" } }
			""".formatted(COURSE_ID);
		apply(MEMBER_ID, noEmergency)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("비상 연락처 이름을 작성해주세요."));

		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L"), false))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("개인정보 제3자 제공에 동의해야 신청할 수 있습니다."))
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		assertThat(participationRepository.count()).isZero();
	}

	@Test
	@DisplayName("대회 당일 만 나이가 종목의 나이 제한 밖이면 400이다")
	void applyOutOfAgeRange() throws Exception {
		String birth = LocalDate.now().minusYears(12).toString();

		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김어린", birth, "S"), true))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("참가할 수 있는 나이가 아닙니다(대회 당일 만 14세 이상, 신청자 만 12세)."))
			.andExpect(jsonPath("$.code").value("ORDER_AGE_RESTRICTED"));

		assertThat(participationRepository.count()).isZero();
	}

	@Test
	@DisplayName("같은 회원이 같은 대회에 살아 있는 신청을 두 건 가질 수 없다")
	void applyTwice() throws Exception {
		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isCreated());

		// 같은 대회의 다른 종목이어도 막는다
		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID + 1, RACE_ID, 14, null));
		apply(MEMBER_ID, applyBody(COURSE_ID + 1, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 이 대회에 신청했습니다."))
			.andExpect(jsonPath("$.code").value("ORDER_DUPLICATE_PARTICIPATION"));

		// 다른 회원은 신청할 수 있다
		apply(OTHER_MEMBER_ID, applyBody(COURSE_ID, participant("박러너", "1990-01-01", "M"), true))
			.andExpect(status().isCreated());
		assertThat(participationRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("끝난 신청(취소 등)만 있으면 같은 대회에 다시 신청할 수 있다")
	void applyAgainAfterCanceled() throws Exception {
		int id = applyAndGetId(MEMBER_ID);
		finish(id, ParticipationStatus.CANCELED);

		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("판매자가 취소한 대회는 접수 기간 안이어도 409 \"취소된 대회입니다.\"로 신청을 막는다")
	void applyCanceledRace() throws Exception {
		raceCoursePort.put(StubRaceCoursePort.canceled(StubRaceCoursePort.openCourse(COURSE_ID, RACE_ID, 14, null)));

		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("취소된 대회입니다."))
			.andExpect(jsonPath("$.code").value("ORDER_RACE_CANCELED"));
	}

	@Test
	@DisplayName("접수 기간이 아니면 409, 없는 종목이면 404다")
	void applyNotAccepting() throws Exception {
		raceCoursePort.put(StubRaceCoursePort.closedCourse(COURSE_ID, RACE_ID));
		apply(MEMBER_ID, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("접수 기간이 아닙니다."))
			.andExpect(jsonPath("$.code").value("ORDER_APPLICATION_CLOSED"));

		apply(MEMBER_ID, applyBody(999, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 종목입니다."))
			.andExpect(jsonPath("$.code").value("ORDER_COURSE_NOT_FOUND"));
	}

	@Test
	@DisplayName("회원 ID가 0 이하면 400이다")
	void applyWithInvalidMemberId() throws Exception {
		apply(0, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("회원 ID가 올바르지 않습니다."))
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("두 번째 신청부터 최근 신청의 참가자 정보가 채워져 내려온다. 신청한 적이 없으면 data가 null이다")
	void latestParticipant() throws Exception {
		mockMvc.perform(get(BASE_URL + "/latest-participant").header("X-Member-Id", MEMBER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("이전에 신청한 참가자 정보가 없습니다."))
			.andExpect(jsonPath("$.data").isEmpty());

		applyAndGetId(MEMBER_ID);

		mockMvc.perform(get(BASE_URL + "/latest-participant").header("X-Member-Id", MEMBER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("최근 참가자 정보를 조회했습니다."))
			.andExpect(jsonPath("$.data.name").value("김러너"))
			.andExpect(jsonPath("$.data.birth").value("1994-06-25"))
			.andExpect(jsonPath("$.data.gender").value("MALE"))
			.andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
			.andExpect(jsonPath("$.data.emergencyContactName").value("김가족"))
			.andExpect(jsonPath("$.data.emergencyContactPhone").value("010-9876-5432"))
			.andExpect(jsonPath("$.data.tshirtSize").value("L"))
			.andExpect(jsonPath("$.data.shippingAddress.address").value(ADDRESS));

		// 다른 회원의 정보는 채워 주지 않는다
		mockMvc.perform(get(BASE_URL + "/latest-participant").header("X-Member-Id", OTHER_MEMBER_ID))
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	@DisplayName("마지막 신청이 키트 없는 종목이면, 다음 신청서의 배송 주소는 주소를 적은 최근 신청에서 채운다")
	void latestParticipantKeepsShippingAddress() throws Exception {
		applyAndGetId(MEMBER_ID);
		raceCoursePort.put(StubRaceCoursePort.courseWithoutKit(COURSE_ID + 1, RACE_ID + 1));
		apply(MEMBER_ID, applyBody(COURSE_ID + 1, participant("김러너", "1994-06-25", "M", null), true))
			.andExpect(status().isCreated());

		mockMvc.perform(get(BASE_URL + "/latest-participant").header("X-Member-Id", MEMBER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tshirtSize").value("M"))
			.andExpect(jsonPath("$.data.shippingAddress.address").value(ADDRESS));
	}

	@Test
	@DisplayName("본인 신청은 조회되고, 다른 회원의 신청은 403, 없는 신청은 404다")
	void findById() throws Exception {
		int id = applyAndGetId(MEMBER_ID);

		mockMvc.perform(get(BASE_URL + "/" + id).header("X-Member-Id", MEMBER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("참가 신청을 조회했습니다."))
			.andExpect(jsonPath("$.data.id").value(id));

		mockMvc.perform(get(BASE_URL + "/" + id).header("X-Member-Id", OTHER_MEMBER_ID))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("본인의 참가 신청만 보거나 고칠 수 있습니다."))
			.andExpect(jsonPath("$.code").value("ORDER_PARTICIPATION_FORBIDDEN"));

		mockMvc.perform(get(BASE_URL + "/999999").header("X-Member-Id", MEMBER_ID))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 참가 신청입니다."))
			.andExpect(jsonPath("$.code").value("ORDER_PARTICIPATION_NOT_FOUND"));
	}

	@Test
	@DisplayName("접수 마감 전에는 참가자 정보가 수정된다")
	void changeParticipantBeforeClose() throws Exception {
		int id = applyAndGetId(MEMBER_ID);

		change(MEMBER_ID, id, participant("김러너", "1994-06-25", "XL", "부산 해운대구 해운대로 00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("참가자 정보를 수정했습니다."))
			.andExpect(jsonPath("$.data.participant.tshirtSize").value("XL"))
			.andExpect(jsonPath("$.data.participant.shippingAddress.address").value("부산 해운대구 해운대로 00"));

		ParticipantInfo saved = participationRepository.findById(id).orElseThrow().getParticipant();
		assertThat(saved.getTshirtSize()).isEqualTo("XL");
		assertThat(saved.getShippingAddress().getAddress()).isEqualTo("부산 해운대구 해운대로 00");

		// 키트가 있는 종목은 수정할 때도 배송 주소를 비울 수 없다
		change(MEMBER_ID, id, participant("김러너", "1994-06-25", "XL", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("키트를 받을 배송 주소를 작성해주세요."))
			.andExpect(jsonPath("$.code").value("ORDER_SHIPPING_ADDRESS_REQUIRED"));
		ParticipantInfo unchanged = participationRepository.findById(id).orElseThrow().getParticipant();
		assertThat(unchanged.getShippingAddress().getAddress()).isEqualTo("부산 해운대구 해운대로 00");
	}

	@Test
	@DisplayName("키트가 없는 종목은 참가자 정보를 고칠 때 보낸 배송지도 저장하지 않는다")
	void changeParticipantCourseWithoutKit() throws Exception {
		raceCoursePort.put(StubRaceCoursePort.courseWithoutKit(COURSE_ID, RACE_ID));
		int id = applyAndGetId(MEMBER_ID);

		change(MEMBER_ID, id, participant("김러너", "1994-06-25", "", "서울 어딘가"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.participant.shippingAddress").isEmpty());
		assertThat(participationRepository.findById(id).orElseThrow().getParticipant().getShippingAddress()).isNull();
	}

	@Test
	@DisplayName("접수 마감 뒤에는 참가자 정보 수정이 409로 거절된다")
	void changeParticipantAfterClose() throws Exception {
		int id = applyAndGetId(MEMBER_ID);
		raceCoursePort.put(StubRaceCoursePort.closedCourse(COURSE_ID, RACE_ID));

		change(MEMBER_ID, id, participant("김러너", "1994-06-25", "XL"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("접수가 마감되어 참가자 정보를 고칠 수 없습니다."))
			.andExpect(jsonPath("$.code").value("ORDER_APPLICATION_CLOSED"));

		assertThat(participationRepository.findById(id).orElseThrow().getParticipant().getTshirtSize())
			.isEqualTo("L");
	}

	@Test
	@DisplayName("다른 회원의 신청은 403, 끝난 신청은 409, 나이 제한 밖으로 고치면 400이다")
	void changeParticipantRejected() throws Exception {
		int id = applyAndGetId(MEMBER_ID);

		change(OTHER_MEMBER_ID, id, participant("박러너", "1990-01-01", "M"))
			.andExpect(status().isForbidden());

		change(MEMBER_ID, id, participant("김러너", LocalDate.now().minusYears(10).toString(), "L"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("참가할 수 있는 나이가 아닙니다(대회 당일 만 14세 이상, 신청자 만 10세)."))
			.andExpect(jsonPath("$.code").value("ORDER_AGE_RESTRICTED"));

		finish(id, ParticipationStatus.CANCELED);
		change(MEMBER_ID, id, participant("김러너", "1994-06-25", "XL"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("참가자 정보를 고칠 수 없는 신청입니다(현재 상태: 취소)."))
			.andExpect(jsonPath("$.code").value("ORDER_PARTICIPATION_NOT_EDITABLE"));
	}

	@Test
	@DisplayName("결제 전 신청서를 취소하면 끝난 신청이 되고, 같은 대회의 다른 종목으로 바로 다시 신청할 수 있다")
	void cancelBeforePayment() throws Exception {
		int id = applyAndGetId(MEMBER_ID);

		cancel(MEMBER_ID, id)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("참가 신청서를 취소했습니다."))
			.andExpect(jsonPath("$.data.id").value(id))
			.andExpect(jsonPath("$.data.status").value("CANCELED"));

		Participation canceled = participationRepository.findById(id).orElseThrow();
		assertThat(canceled.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
		assertThat(canceled.getActiveFlag()).isNull();
		assertThat(canceled.getCanceledAt()).isNotNull();

		raceCoursePort.put(StubRaceCoursePort.openCourse(COURSE_ID + 1, RACE_ID, 14, null));
		apply(MEMBER_ID, applyBody(COURSE_ID + 1, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.raceCourseId").value(COURSE_ID + 1));
	}

	@Test
	@DisplayName("다른 회원의 신청서는 403, 없는 신청서는 404이고 신청서는 그대로 남는다")
	void cancelBeforePaymentNotOwned() throws Exception {
		int id = applyAndGetId(MEMBER_ID);

		cancel(OTHER_MEMBER_ID, id)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("본인의 참가 신청만 보거나 고칠 수 있습니다."))
			.andExpect(jsonPath("$.code").value("ORDER_PARTICIPATION_FORBIDDEN"));
		cancel(MEMBER_ID, id + 1000)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 참가 신청입니다."))
			.andExpect(jsonPath("$.code").value("ORDER_PARTICIPATION_NOT_FOUND"));

		assertThat(participationRepository.findById(id).orElseThrow().getStatus())
			.isEqualTo(ParticipationStatus.PENDING);
	}

	@Test
	@DisplayName("결제 대기·결제 완료·이미 끝난 신청서는 409이고 상태가 바뀌지 않는다")
	void cancelBeforePaymentRejected() throws Exception {
		int id = applyAndGetId(MEMBER_ID);

		moveTo(id, ParticipationStatus.APPLIED);
		cancel(MEMBER_ID, id)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("결제 대기 중인 신청은 취소할 수 없습니다. 결제하지 않으면 결제 기한이 지난 뒤 자동으로 풀립니다."))
			.andExpect(jsonPath("$.code").value("ORDER_PARTICIPATION_NOT_CANCELABLE"));
		assertThat(participationRepository.findById(id).orElseThrow().getStatus())
			.isEqualTo(ParticipationStatus.APPLIED);

		moveTo(id, ParticipationStatus.CONFIRMED);
		cancel(MEMBER_ID, id)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("결제한 참가권은 주문 상품 취소로 취소해주세요."))
			.andExpect(jsonPath("$.code").value("ORDER_PARTICIPATION_NOT_CANCELABLE"));
		assertThat(participationRepository.findById(id).orElseThrow().getStatus())
			.isEqualTo(ParticipationStatus.CONFIRMED);

		moveTo(id, ParticipationStatus.PENDING);
		cancel(MEMBER_ID, id).andExpect(status().isOk());
		cancel(MEMBER_ID, id)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 끝난 신청입니다(현재 상태: 취소)."))
			.andExpect(jsonPath("$.code").value("ORDER_PARTICIPATION_NOT_CANCELABLE"));
	}

	private ResultActions apply(int memberId, String body) throws Exception {
		return mockMvc.perform(post(BASE_URL)
			.header("X-Member-Id", memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions change(int memberId, int participationId, String participantBody) throws Exception {
		return mockMvc.perform(put(BASE_URL + "/" + participationId + "/participant")
			.header("X-Member-Id", memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(participantBody));
	}

	private ResultActions cancel(int memberId, int participationId) throws Exception {
		return mockMvc.perform(post(BASE_URL + "/" + participationId + "/cancel")
			.header("X-Member-Id", memberId));
	}

	// [결제하기]·결제는 자리·예치금 스텁이 필요해, 이 테스트에서는 살아 있는 상태로 직접 옮긴다
	private void moveTo(int participationId, ParticipationStatus status) {
		Participation participation = participationRepository.findById(participationId).orElseThrow();
		ReflectionTestUtils.setField(participation, "status", status);
		ReflectionTestUtils.setField(participation, "activeFlag", status.isActive() ? Boolean.TRUE : null);
		participationRepository.save(participation);
	}

	private int applyAndGetId(int memberId) throws Exception {
		apply(memberId, applyBody(COURSE_ID, participant("김러너", "1994-06-25", "L"), true))
			.andExpect(status().isCreated());
		return participationRepository.findFirstByMemberIdOrderByIdDesc(memberId).orElseThrow().getId();
	}

	// 상태 전이 API(결제·취소)가 아직 없어 테스트에서 직접 끝난 신청으로 만든다
	private void finish(int participationId, ParticipationStatus status) {
		Participation participation = participationRepository.findById(participationId).orElseThrow();
		ReflectionTestUtils.setField(participation, "status", status);
		ReflectionTestUtils.setField(participation, "activeFlag", null);
		participationRepository.save(participation);
	}

	private static String applyBody(int raceCourseId, String participant, boolean consent) {
		return """
			{ "raceCourseId": %d, "participant": %s, "thirdPartyConsent": %s }
			""".formatted(raceCourseId, participant, consent);
	}

	private static String participant(String name, String birth, String tshirtSize) {
		return participant(name, birth, tshirtSize, ADDRESS);
	}

	// address가 null이면 배송지(shippingAddress)를 보내지 않는다
	private static String participant(String name, String birth, String tshirtSize, String address) {
		String shipping = address == null ? "" : """
			, "shippingAddress": { "recipientName": "김러너", "recipientPhone": "010-1234-5678", "zipcode": "04700",
				"address": "%s", "addressDetail": "101동 1001호" }""".formatted(address);
		return """
			{ "name": "%s", "birth": "%s", "gender": "MALE", "phone": "010-1234-5678",
				"emergencyContactName": "김가족", "emergencyContactPhone": "010-9876-5432", "tshirtSize": "%s"%s }
			""".formatted(name, birth, tshirtSize, shipping);
	}
}
