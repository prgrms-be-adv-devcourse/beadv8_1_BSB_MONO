package com.bukang.product.in;

import static com.bukang.product.support.RaceRequests.course;
import static com.bukang.product.support.RaceRequests.image;
import static com.bukang.product.support.RaceRequests.race;
import static com.bukang.product.support.RaceRequests.thumbnail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.bukang.common.shared.file.domain.FileType;
import com.bukang.common.shared.file.event.FileLinkRequestedEvent;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.product.domain.CourseStatus;
import com.bukang.product.domain.CourseSurface;
import com.bukang.product.domain.CourseType;
import com.bukang.product.domain.Race;
import com.bukang.product.domain.RaceCourse;
import com.bukang.product.domain.RaceSaleStatus;
import com.bukang.product.domain.TshirtSize;
import com.bukang.product.domain.document.RaceCourseDocument;
import com.bukang.product.dto.RaceImageRequestDto;
import com.bukang.product.dto.RaceSaveRequestDto;
import com.bukang.product.support.ProductIntegrationTest;
import com.bukang.product.support.RaceRequests.CourseBuilder;
import com.bukang.product.support.RaceRequests.RaceBuilder;
import com.jayway.jsonpath.JsonPath;

/**
 * 대회 수정(PUT /api/v1/product/races/{raceId})과 판매 시작(POST /api/v1/product/races/{raceId}/sale)을 확인한다.
 * - 판매 시작 전(작성 중, 판매 예정이지만 판매 시작 일시 전)에는 참가비·정원·참가 조건을 자유롭게 바꿀 수 있다
 * - 판매가 시작된 뒤에는 참가비 인상, 정원 줄이기, 참가 조건·티셔츠 사이즈 변경, 환불 기준일 앞당기기, 종목 빼기가 409다
 * - 판매 시작을 누른 뒤에는 이미 지난 판매 일정을 바꾸거나 일정을 지난 시각으로 옮기면 409다
 * - 판매가 시작된 뒤 대회 일시·장소를 바꾸면 일정·장소 변경 일시를 남긴다 (판매 전에는 남기지 않는다)
 * - 수정이 커밋되면 파일 연결 메시지를 보내고 검색 문서를 다시 만들며, 정원을 늘리면 정원 마감 종목이 접수 중으로 돌아온다
 * - 판매 시작은 판매 시작 일시 전이면 판매 예정, 지났으면 판매 중이 되고, 작성 중이 아닌 대회는 409다
 */
class ApiV1RaceUpdateTest extends ProductIntegrationTest {
	private static final String RACES = "/api/v1/product/races/";
	// 판매 시작 응답 메시지의 일시 형식 (ApiV1RaceController와 같게)
	private static final DateTimeFormatter MESSAGE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	// 등록 요청과 수정 요청이 같은 일정을 쓰도록 기준 시각을 테스트마다 하나로 고정한다
	// (race()는 부를 때마다 지금 시각을 새로 잡아, 따로 만들면 일정이 몇 초씩 달라져 일정 변경으로 보인다)
	private final LocalDateTime now = LocalDateTime.now().withNano(0);

	// ---------- 판매 시작 전 수정 ----------

	@Test
	@DisplayName("작성 중 대회는 참가비 인상(45,000→60,000원), 정원 축소(100→50), 참가 조건·환불 기준일·지난 일정을 모두 바꿀 수 있다")
	void updateAnythingWhileDraft() throws Exception {
		RegisteredRace registered = register(sameRace().build());

		RaceSaveRequestDto request = sameRace()
			.individualOpenAt(now.minusHours(2))
			.refundDeadline(now.plusDays(5).toLocalDate())
			.course(sameCourse(registered.courseId())
				.price(60_000)
				.quota(0, 50)
				.type(CourseType.HALF)
				.surface(CourseSurface.TRAIL)
				.distanceKm("21.1")
				.timeLimitMinutes(180)
				.age(20, 60)
				.kit(5_000, TshirtSize.L, TshirtSize.M))
			.build();

		update(registered.raceId(), request)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("대회를 수정했습니다."))
			.andExpect(jsonPath("$.data.raceId").value(registered.raceId()))
			.andExpect(jsonPath("$.data.saleStatus").value("DRAFT"))
			.andExpect(jsonPath("$.data.courseIds.length()").value(1))
			.andExpect(jsonPath("$.data.courseIds[0]").value(registered.courseId()));

		RaceCourse course = findCourse(registered.courseId());
		assertThat(course.getPrice()).isEqualTo(60_000);
		assertThat(course.getIndividualQuota()).isEqualTo(50);
		assertThat(course.getCourseType()).isEqualTo(CourseType.HALF);
		assertThat(course.getSurface()).isEqualTo(CourseSurface.TRAIL);
		assertThat(course.getDistanceKm()).isEqualByComparingTo(new BigDecimal("21.1"));
		assertThat(course.getTimeLimitMinutes()).isEqualTo(180);
		assertThat(course.getMinAge()).isEqualTo(20);
		assertThat(course.getMaxAge()).isEqualTo(60);
		assertThat(course.isKitIncluded()).isTrue();
		// 요청 순서(L, M)와 상관없이 작은 사이즈부터 저장한다
		assertThat(course.getTshirtSizeOption()).containsExactly(TshirtSize.M, TshirtSize.L);
		Race race = findRace(registered.raceId());
		assertThat(race.getRefundDeadline()).isEqualTo(now.plusDays(5).toLocalDate());
		assertThat(race.getIndividualOpenAt()).isEqualTo(now.minusHours(2));
		// 판매 시작 일시는 바뀐 개인 예매 오픈으로 다시 채운다
		assertThat(race.getSaleOpenAt()).isEqualTo(now.minusHours(2));
	}

	@Test
	@DisplayName("작성 중 대회의 대회 일시를 15일 뒤에서 16일 뒤로 늦추고 장소를 바꿔도 일정·장소 변경 일시는 남기지 않는다")
	void noScheduleChangeRecordWhileDraft() throws Exception {
		RegisteredRace registered = register(sameRace().build());

		update(registered.raceId(), sameRace()
			.raceDate(now.plusDays(16))
			.location("뚝섬 한강공원")
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk());

		Race race = findRace(registered.raceId());
		assertThat(race.getRaceDate()).isEqualTo(now.plusDays(16));
		assertThat(race.getLocation()).isEqualTo("뚝섬 한강공원");
		assertThat(race.getScheduleChangedAt()).isNull();
	}

	@Test
	@DisplayName("판매 예정(오픈 하루 전) 대회는 참가비 인상·정원 축소·대회 일시 변경이 되고 일정·장소 변경 일시를 남기지 않는다")
	void updateScheduledRaceBeforeSaleOpen() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().individualOpenAt(now.plusDays(1)).build());

		update(registered.raceId(), sameRace()
			.individualOpenAt(now.plusDays(1))
			.raceDate(now.plusDays(16))
			.refundDeadline(now.plusDays(15).toLocalDate())
			.course(sameCourse(registered.courseId()).price(60_000).quota(0, 50))
			.build())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.saleStatus").value("SCHEDULED"));

		RaceCourse course = findCourse(registered.courseId());
		assertThat(course.getPrice()).isEqualTo(60_000);
		assertThat(course.getIndividualQuota()).isEqualTo(50);
		Race race = findRace(registered.raceId());
		assertThat(race.getRaceDate()).isEqualTo(now.plusDays(16));
		assertThat(race.getScheduleChangedAt()).isNull();
	}

	@Test
	@DisplayName("판매 예정 대회의 개인 예매 오픈(하루 뒤)을 한 시간 전으로 옮기면 409이고 일정은 그대로다")
	void rejectMovingScheduleToPastWhenScheduled() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().individualOpenAt(now.plusDays(1)).build());

		update(registered.raceId(), sameRace()
			.individualOpenAt(now.minusHours(1))
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value("개인 예매 오픈 일시를 지난 시각으로 바꿀 수 없습니다."));

		assertThat(findRace(registered.raceId()).getIndividualOpenAt()).isEqualTo(now.plusDays(1));
	}

	// ---------- 판매 시작 뒤 수정: 막히는 변경 ----------

	@Test
	@DisplayName("판매 중 종목의 참가비를 45,000원에서 50,000원으로 올리면 409이고, 저장되지 않아 파일 연결 메시지도 없다")
	void rejectPriceRaiseAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace().course(sameCourse(registered.courseId()).price(50_000)).build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value("판매가 시작된 종목은 참가비를 올릴 수 없습니다. (종목: 10K)"));

		assertThat(findCourse(registered.courseId()).getPrice()).isEqualTo(45_000);
		// 등록 때 보낸 1건뿐이다
		assertThat(fileLinkMessages()).hasSize(1);
	}

	@Test
	@DisplayName("판매 중 종목의 개인 정원을 100에서 99로 줄이면 409이다")
	void rejectIndividualQuotaDecreaseAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace().course(sameCourse(registered.courseId()).quota(0, 99)).build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("판매가 시작된 종목은 정원을 줄일 수 없습니다. (종목: 10K)"));

		assertThat(findCourse(registered.courseId()).getIndividualQuota()).isEqualTo(100);
	}

	@Test
	@DisplayName("크루 예매가 있는 판매 중 종목의 크루 정원을 10에서 9로 줄이면 409이다 (개인 정원 100은 그대로)")
	void rejectCrewQuotaDecreaseAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(withPassedCrewSale(sameRace())
			.course(course().quota(10, 100))
			.build());

		update(registered.raceId(), withPassedCrewSale(sameRace())
			.course(sameCourse(registered.courseId()).quota(9, 100))
			.build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("판매가 시작된 종목은 정원을 줄일 수 없습니다. (종목: 10K)"));

		assertThat(findCourse(registered.courseId()).getCrewQuota()).isEqualTo(10);
	}

	static Stream<Arguments> courseConditionChanges() {
		return Stream.of(
			conditionChange("거리", "10km → 10.5km", builder -> builder.distanceKm("10.5")),
			conditionChange("종목 구분", "KM_10 → HALF", builder -> builder.type(CourseType.HALF)),
			conditionChange("노면", "ROAD → TRAIL", builder -> builder.surface(CourseSurface.TRAIL)),
			conditionChange("제한 시간", "90분 → 100분", builder -> builder.timeLimitMinutes(100)),
			conditionChange("나이 제한", "최소 14세 → 20세", builder -> builder.age(20, null)),
			conditionChange("나이 제한", "최대 없음 → 60세", builder -> builder.age(14, 60)),
			conditionChange("키트 포함 여부", "키트 없음 → 포함", builder -> builder.kit(0))
		);
	}

	private static Arguments conditionChange(String condition, String change, UnaryOperator<CourseBuilder> edit) {
		return Arguments.of(condition, change, edit);
	}

	@ParameterizedTest(name = "{0}: {1}")
	@MethodSource("courseConditionChanges")
	@DisplayName("판매 중 종목의 참가 조건을 하나 바꾸면 409이고 메시지에 바뀐 조건 이름이 나온다")
	void rejectConditionChangeAfterSaleStarted(String condition, String change, UnaryOperator<CourseBuilder> edit)
		throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace().course(edit.apply(sameCourse(registered.courseId()))).build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message")
				.value("판매가 시작된 종목은 참가 조건(%s)을 바꿀 수 없습니다. (종목: 10K)".formatted(condition)));
	}

	@Test
	@DisplayName("판매 중 종목의 티셔츠 사이즈를 S·M·L에서 S·M으로 바꾸면 409이다")
	void rejectTshirtSizeChangeAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace()
			.course(course().kit(5_000, TshirtSize.S, TshirtSize.M, TshirtSize.L))
			.build());

		update(registered.raceId(), sameRace()
			.course(sameCourse(registered.courseId()).kit(5_000, TshirtSize.S, TshirtSize.M))
			.build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("판매가 시작된 종목은 티셔츠 사이즈를 바꿀 수 없습니다. (종목: 10K)"));

		assertThat(findCourse(registered.courseId()).getTshirtSizeOption())
			.containsExactly(TshirtSize.S, TshirtSize.M, TshirtSize.L);
	}

	@Test
	@DisplayName("판매 중 대회의 환불 기준일을 대회 날짜(15일 뒤)에서 14일 뒤로 앞당기면 409이다")
	void rejectRefundDeadlineAdvanceAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace()
			.refundDeadline(now.plusDays(14).toLocalDate())
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("판매가 시작된 대회는 환불 기준일을 앞당길 수 없습니다."));

		assertThat(findRace(registered.raceId()).getRefundDeadline()).isEqualTo(now.plusDays(15).toLocalDate());
	}

	@Test
	@DisplayName("판매 중 대회의 이미 지난 개인 예매 오픈(한 시간 전)을 두 시간 전으로 바꾸면 409이다")
	void rejectPassedIndividualOpenChange() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace()
			.individualOpenAt(now.minusHours(2))
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 지난 개인 예매 오픈 일시는 바꿀 수 없습니다."));

		assertThat(findRace(registered.raceId()).getIndividualOpenAt()).isEqualTo(now.minusHours(1));
	}

	@Test
	@DisplayName("판매 중 대회의 이미 지난 크루 예매 시작(3일 전)을 한 시간 늦추면 409이다")
	void rejectPassedCrewSaleStartChange() throws Exception {
		RegisteredRace registered = registerOnSale(withPassedCrewSale(sameRace()).build());

		update(registered.raceId(), sameRace()
			.crewSale(now.minusDays(3).plusHours(1), now.minusDays(2))
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 지난 크루 예매 시작 일시는 바꿀 수 없습니다."));

		assertThat(findRace(registered.raceId()).getCrewSaleStartAt()).isEqualTo(now.minusDays(3));
	}

	@Test
	@DisplayName("판매 중 대회의 접수 마감(10일 뒤)을 30분 전으로 옮기면 409이다")
	void rejectMovingApplicationCloseToPast() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace()
			.applicationCloseAt(now.minusMinutes(30))
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("접수 마감 일시를 지난 시각으로 바꿀 수 없습니다."));

		assertThat(findRace(registered.raceId()).getApplicationCloseAt()).isEqualTo(now.plusDays(10));
	}

	@Test
	@DisplayName("판매 중 대회에서 10K·하프 중 하프 종목을 빼면 409이고 두 종목 모두 남는다")
	void rejectCourseRemovalAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().course(course(), halfCourse()).build());
		int halfCourseId = registered.courseIds().get(1);

		update(registered.raceId(), sameRace().course(sameCourse(registered.courseId())).build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("판매가 시작된 대회는 종목을 뺄 수 없습니다. (종목: 하프)"));

		assertThat(raceCourseRepository.existsById(registered.courseId())).isTrue();
		assertThat(raceCourseRepository.existsById(halfCourseId)).isTrue();
	}

	@Test
	@DisplayName("판매 중 대회를 다른 판매자(회원 9002)가 수정하면 403이고 대회는 그대로다")
	void rejectUpdateByOtherSeller() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		putJson(RACES + registered.raceId(), OTHER_SELLER_MEMBER_ID, sameRace()
			.title("남의 대회 이름 바꾸기")
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.message").value("본인이 등록한 대회만 처리할 수 있습니다."));

		assertThat(findRace(registered.raceId()).getTitle()).isEqualTo("2026 서울 하프 마라톤");
	}

	@Test
	@DisplayName("판매자가 아닌 회원(9999)이 수정하면 403이다")
	void rejectUpdateByNonSeller() throws Exception {
		RegisteredRace registered = register(sameRace().build());

		putJson(RACES + registered.raceId(), NOT_SELLER_MEMBER_ID, sameRace()
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("승인된 판매자만 대회를 등록·수정할 수 있습니다."));
	}

	@Test
	@DisplayName("회원 ID 헤더 없이 수정하면 401이다")
	void rejectUpdateWithoutMemberHeader() throws Exception {
		RegisteredRace registered = register(sameRace().build());

		putJson(RACES + registered.raceId(), null, sameRace().course(sameCourse(registered.courseId())).build())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("없는 대회(ID 999999)를 수정하면 404이다")
	void rejectUpdateOfMissingRace() throws Exception {
		update(999_999, sameRace().build())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.message").value("존재하지 않는 대회입니다."));
	}

	@Test
	@DisplayName("판매 중에 취소한 대회를 수정하면 409이고 대회는 그대로다")
	void rejectUpdateOfCanceledRace() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());
		postAs(RACES + registered.raceId() + "/cancel", SELLER_MEMBER_ID).andExpect(status().isOk());

		update(registered.raceId(), sameRace()
			.title("취소 뒤 바꾼 이름")
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("취소된 대회는 수정할 수 없습니다."));

		Race race = findRace(registered.raceId());
		assertThat(race.getSaleStatus()).isEqualTo(RaceSaleStatus.CANCELED);
		assertThat(race.getTitle()).isEqualTo("2026 서울 하프 마라톤");
	}

	// ---------- 판매 시작 뒤 수정: 허용되는 변경 ----------

	@Test
	@DisplayName("판매 중에도 참가비를 45,000원에서 40,000원으로 내리고 개인 정원을 100에서 150으로 늘릴 수 있다")
	void allowFavorableChangesAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace()
			.course(sameCourse(registered.courseId()).price(40_000).quota(0, 150))
			.build())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.saleStatus").value("ON_SALE"));

		RaceCourse course = findCourse(registered.courseId());
		assertThat(course.getPrice()).isEqualTo(40_000);
		assertThat(course.getIndividualQuota()).isEqualTo(150);
	}

	@Test
	@DisplayName("판매 중에도 종목명은 '10K'에서 '10K 로드'로 바꿀 수 있고, 거리 10을 10.00으로 보내면 같은 거리로 본다")
	void allowCourseNameChangeAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace()
			.course(sameCourse(registered.courseId()).name("10K 로드").distanceKm("10.00"))
			.build())
			.andExpect(status().isOk());

		assertThat(findCourse(registered.courseId()).getName()).isEqualTo("10K 로드");
	}

	@Test
	@DisplayName("판매 중 종목의 티셔츠 사이즈 S·M·L을 L·S·M으로 순서만 바꿔 보내면 통과하고 S·M·L 그대로 저장된다")
	void allowTshirtSizeReorderAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace()
			.course(course().kit(5_000, TshirtSize.S, TshirtSize.M, TshirtSize.L))
			.build());

		update(registered.raceId(), sameRace()
			.course(sameCourse(registered.courseId()).kit(5_000, TshirtSize.L, TshirtSize.S, TshirtSize.M))
			.build())
			.andExpect(status().isOk());

		assertThat(findCourse(registered.courseId()).getTshirtSizeOption())
			.containsExactly(TshirtSize.S, TshirtSize.M, TshirtSize.L);
	}

	@Test
	@DisplayName("판매 중 대회의 환불 기준일을 대회 5일 전에서 2일 전으로 늦추는 것은 된다")
	void allowRefundDeadlineDelayAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().refundDeadline(now.plusDays(10).toLocalDate()).build());

		update(registered.raceId(), sameRace()
			.refundDeadline(now.plusDays(13).toLocalDate())
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk());

		assertThat(findRace(registered.raceId()).getRefundDeadline()).isEqualTo(now.plusDays(13).toLocalDate());
	}

	@Test
	@DisplayName("판매 중에도 아직 오지 않은 접수 마감은 10일 뒤에서 5일 뒤로 앞당길 수 있다")
	void allowFutureScheduleChangeAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace()
			.applicationCloseAt(now.plusDays(5))
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk());

		assertThat(findRace(registered.raceId()).getApplicationCloseAt()).isEqualTo(now.plusDays(5));
	}

	// ---------- 종목 추가·빼기와 종목 ID ----------

	@Test
	@DisplayName("판매 중 대회에 courseId 없는 5K 종목을 기존 10K 앞에 넣으면 새 종목으로 추가되고 응답 courseIds도 요청 순서다")
	void addCourseWithoutCourseIdAfterSaleStarted() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		MvcResult result = update(registered.raceId(), sameRace()
			.course(fiveKmCourse(), sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courseIds.length()").value(2))
			.andExpect(jsonPath("$.data.courseIds[1]").value(registered.courseId()))
			.andReturn();
		int newCourseId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.courseIds[0]");

		assertThat(newCourseId).isNotEqualTo(registered.courseId());
		RaceCourse added = findCourse(newCourseId);
		assertThat(added.getName()).isEqualTo("5K");
		assertThat(added.getStatus()).isEqualTo(CourseStatus.OPEN);
		assertThat(findDocument(newCourseId).getSaleStatus()).isEqualTo(RaceSaleStatus.ON_SALE);
		// 새 종목의 이미지도 요청 순서대로 연결을 요청한다
		assertThat(lastFileLink().refs())
			.extracting(FileLinkRequestedEvent.Ref::refId)
			.containsExactly(newCourseId, registered.courseId());
	}

	@Test
	@DisplayName("작성 중 대회에서 하프 종목을 빼면 종목과 검색 문서가 지워지고, 파일 연결 메시지에 빈 목록을 보내 이미지를 뗀다")
	void removeCourseWhileDraft() throws Exception {
		RegisteredRace registered = register(sameRace().course(course(), halfCourse()).build());
		int halfCourseId = registered.courseIds().get(1);
		assertThat(raceCourseDocumentRepository.existsById(String.valueOf(halfCourseId))).isTrue();

		update(registered.raceId(), sameRace().course(sameCourse(registered.courseId())).build())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courseIds.length()").value(1))
			.andExpect(jsonPath("$.data.courseIds[0]").value(registered.courseId()));

		assertThat(raceCourseRepository.existsById(halfCourseId)).isFalse();
		assertThat(raceCourseDocumentRepository.existsById(String.valueOf(halfCourseId))).isFalse();
		FileLinkRequestedEvent fileLink = lastFileLink();
		assertThat(refOf(fileLink, halfCourseId).files()).isEmpty();
		assertThat(refOf(fileLink, registered.courseId()).files()).hasSize(1);
	}

	@Test
	@DisplayName("다른 대회의 종목 ID를 courseId로 보내면 400이고 두 대회의 종목은 그대로다")
	void rejectCourseIdOfOtherRace() throws Exception {
		RegisteredRace registered = register(sameRace().build());
		RegisteredRace other = register(sameRace().title("2026 부산 바다 런").build());

		update(registered.raceId(), sameRace().course(sameCourse(other.courseId())).build())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value(
				"이 대회의 종목이 아니거나 같은 종목을 두 번 보냈습니다. (courseId: %d)".formatted(other.courseId())));

		assertThat(raceCourseRepository.existsById(registered.courseId())).isTrue();
		assertThat(raceCourseRepository.existsById(other.courseId())).isTrue();
	}

	@Test
	@DisplayName("같은 종목 ID를 두 번 보내면 400이다")
	void rejectSameCourseIdTwice() throws Exception {
		RegisteredRace registered = register(sameRace().build());

		update(registered.raceId(), sameRace()
			.course(sameCourse(registered.courseId()), sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(
				"이 대회의 종목이 아니거나 같은 종목을 두 번 보냈습니다. (courseId: %d)".formatted(registered.courseId())));
	}

	// ---------- 판매 시작 뒤 일정·장소 변경 기록 ----------

	@Test
	@DisplayName("판매 중 대회의 대회 일시를 15일 뒤에서 16일 뒤로 늦추면 수정한 시각을 일정·장소 변경 일시로 남긴다")
	void recordScheduleChangeWhenRaceDateDelayed() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());
		assertThat(findRace(registered.raceId()).getScheduleChangedAt()).isNull();

		// DB 시각 정밀도(마이크로초)와 상관없게 앞뒤로 1초 여유를 둔다
		LocalDateTime before = LocalDateTime.now().minusSeconds(1);
		update(registered.raceId(), sameRace()
			.raceDate(now.plusDays(16))
			.refundDeadline(now.plusDays(15).toLocalDate())
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk());
		LocalDateTime after = LocalDateTime.now().plusSeconds(1);

		Race race = findRace(registered.raceId());
		assertThat(race.getRaceDate()).isEqualTo(now.plusDays(16));
		assertThat(race.getScheduleChangedAt()).isBetween(before, after);
	}

	@Test
	@DisplayName("판매 중 대회의 대회 일시를 15일 뒤에서 14일 뒤로 앞당겨도(환불 기준일 10일 뒤) 일정·장소 변경 일시를 남긴다")
	void recordScheduleChangeWhenRaceDateAdvanced() throws Exception {
		LocalDate refundDeadline = now.plusDays(10).toLocalDate();
		RegisteredRace registered = registerOnSale(sameRace().refundDeadline(refundDeadline).build());

		update(registered.raceId(), sameRace()
			.raceDate(now.plusDays(14))
			.refundDeadline(refundDeadline)
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk());

		Race race = findRace(registered.raceId());
		assertThat(race.getRaceDate()).isEqualTo(now.plusDays(14));
		assertThat(race.getScheduleChangedAt()).isNotNull();
	}

	@Test
	@DisplayName("판매 중 대회의 장소를 바꾸면 변경 일시를 남기고, 다시 시·군·구만 성동구→광진구로 바꾸면 그 시각으로 새로 남긴다")
	void recordScheduleChangeWhenPlaceChanged() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace()
			.location("뚝섬 한강공원")
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk());
		LocalDateTime firstChangedAt = findRace(registered.raceId()).getScheduleChangedAt();
		assertThat(firstChangedAt).isNotNull();

		update(registered.raceId(), sameRace()
			.location("뚝섬 한강공원")
			.region("서울특별시", "광진구")
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk());

		assertThat(findRace(registered.raceId()).getScheduleChangedAt()).isAfter(firstChangedAt);
	}

	@Test
	@DisplayName("판매 중 대회의 이름과 소개만 바꾸면 일정·장소 변경 일시를 남기지 않는다")
	void noScheduleChangeRecordWhenOnlyTitleChanged() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		update(registered.raceId(), sameRace()
			.title("2026 서울 가을 하프 마라톤")
			.description("소개 글만 고쳤다")
			.course(sameCourse(registered.courseId()))
			.build())
			.andExpect(status().isOk());

		Race race = findRace(registered.raceId());
		assertThat(race.getTitle()).isEqualTo("2026 서울 가을 하프 마라톤");
		assertThat(race.getScheduleChangedAt()).isNull();
	}

	// ---------- 수정 뒤 파일 연결 메시지·검색 문서·종목 상태 ----------

	@Test
	@DisplayName("수정이 커밋되면 새 이미지 목록(대표·소개 이미지)으로 파일 연결 메시지를 보내고 검색 문서를 새 대회명·참가비로 다시 만든다")
	void publishFileLinkAndReindexAfterUpdate() throws Exception {
		RegisteredRace registered = register(sameRace().build());
		RaceImageRequestDto newThumbnail = thumbnail();
		RaceImageRequestDto detail = image(FileType.DETAIL, 0);

		update(registered.raceId(), sameRace()
			.title("2026 부산 바다 런")
			.course(sameCourse(registered.courseId()).price(50_000).images(newThumbnail, detail))
			.build())
			.andExpect(status().isOk());

		// 등록 때 1건, 수정 때 1건
		assertThat(fileLinkMessages()).hasSize(2);
		FileLinkRequestedEvent fileLink = lastFileLink();
		assertThat(fileLink.ownerId()).isEqualTo(SELLER_MEMBER_ID);
		assertThat(fileLink.refs()).hasSize(1);
		FileLinkRequestedEvent.Ref ref = fileLink.refs().getFirst();
		assertThat(ref.refType()).isEqualTo("RaceCourse");
		assertThat(ref.refId()).isEqualTo(registered.courseId());
		assertThat(ref.files())
			.extracting(FileLinkRequestedEvent.Item::fileId)
			.containsExactly(newThumbnail.getFileId(), detail.getFileId());

		RaceCourseDocument document = findDocument(registered.courseId());
		assertThat(document.getRaceTitle()).isEqualTo("2026 부산 바다 런");
		assertThat(document.getCourseLabel()).isEqualTo("2026 부산 바다 런 10K");
		assertThat(document.getPrice()).isEqualTo(50_000);
	}

	@Test
	@DisplayName("개인 정원 1석이 확보돼 정원 마감(SOLD_OUT)된 종목은 정원을 2석으로 늘리면 접수 중(OPEN)으로 돌아오고 검색 문서도 바뀐다")
	void reopenSoldOutCourseWhenQuotaIncreased() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().course(course().quota(0, 1)).build());
		hold(registered.courseId(), 5001)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.held").value(true));
		assertThat(findCourse(registered.courseId()).getStatus()).isEqualTo(CourseStatus.SOLD_OUT);

		// 정원을 그대로 두고 대회명만 바꾸면 정원 마감 그대로이고, 검색 문서도 DB의 정원 마감으로 다시 만든다
		update(registered.raceId(), sameRace()
			.title("2026 서울 가을 하프 마라톤")
			.course(sameCourse(registered.courseId()).quota(0, 1))
			.build())
			.andExpect(status().isOk());
		assertThat(findCourse(registered.courseId()).getStatus()).isEqualTo(CourseStatus.SOLD_OUT);
		assertThat(findDocument(registered.courseId()).getCourseStatus()).isEqualTo(CourseStatus.SOLD_OUT);

		update(registered.raceId(), sameRace()
			.course(sameCourse(registered.courseId()).quota(0, 2))
			.build())
			.andExpect(status().isOk());

		RaceCourse course = findCourse(registered.courseId());
		assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);
		assertThat(course.getIndividualQuota()).isEqualTo(2);
		// 수정은 확보 수를 덮어쓰지 않는다
		assertThat(course.getIndividualReserved()).isEqualTo(1);
		assertThat(findDocument(registered.courseId()).getCourseStatus()).isEqualTo(CourseStatus.OPEN);
	}

	// ---------- 판매 시작 ----------

	@Test
	@DisplayName("개인 예매 오픈이 이틀 뒤인 대회의 판매를 시작하면 판매 예정(SCHEDULED)이 되고 'yyyy-MM-dd HH:mm에 판매가 시작됩니다.'로 알린다")
	void startSaleBeforeOpenBecomesScheduled() throws Exception {
		LocalDateTime openAt = now.plusDays(2);
		RegisteredRace registered = register(sameRace().individualOpenAt(openAt).build());
		String expectedMessage = "%s에 판매가 시작됩니다.".formatted(openAt.format(MESSAGE_DATE_TIME));

		startSaleAs(registered.raceId(), SELLER_MEMBER_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value(expectedMessage))
			.andExpect(jsonPath("$.data.raceId").value(registered.raceId()))
			.andExpect(jsonPath("$.data.saleStatus").value("SCHEDULED"))
			.andExpect(jsonPath("$.data.courseIds[0]").value(registered.courseId()));

		Race race = findRace(registered.raceId());
		assertThat(race.getSaleStatus()).isEqualTo(RaceSaleStatus.SCHEDULED);
		assertThat(race.getSaleOpenAt()).isEqualTo(openAt);
		assertThat(findDocument(registered.courseId()).getSaleStatus()).isEqualTo(RaceSaleStatus.SCHEDULED);
		// 판매 시작은 이미지를 바꾸지 않아 파일 연결 메시지를 보내지 않는다 (등록 때 1건뿐)
		assertThat(fileLinkMessages()).hasSize(1);
	}

	@Test
	@DisplayName("개인 예매 오픈이 한 시간 전에 지난 대회의 판매를 시작하면 바로 판매 중(ON_SALE)이 되고 '판매를 시작했습니다.'로 알린다")
	void startSaleAfterOpenBecomesOnSale() throws Exception {
		RegisteredRace registered = register(sameRace().build());

		startSaleAs(registered.raceId(), SELLER_MEMBER_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("판매를 시작했습니다."))
			.andExpect(jsonPath("$.data.saleStatus").value("ON_SALE"));

		assertThat(findRace(registered.raceId()).getSaleStatus()).isEqualTo(RaceSaleStatus.ON_SALE);
		assertThat(findDocument(registered.courseId()).getSaleStatus()).isEqualTo(RaceSaleStatus.ON_SALE);
	}

	@Test
	@DisplayName("이미 판매 중인 대회의 판매를 다시 시작하면 409이고 판매 중 그대로다")
	void rejectStartSaleWhenAlreadyOnSale() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().build());

		startSaleAs(registered.raceId(), SELLER_MEMBER_ID)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value("작성 중인 대회만 판매를 시작할 수 있습니다."));

		assertThat(findRace(registered.raceId()).getSaleStatus()).isEqualTo(RaceSaleStatus.ON_SALE);
	}

	@Test
	@DisplayName("판매 예정(오픈 이틀 뒤) 대회의 판매를 다시 시작하면 409이고 판매 예정 그대로다")
	void rejectStartSaleWhenScheduled() throws Exception {
		RegisteredRace registered = registerOnSale(sameRace().individualOpenAt(now.plusDays(2)).build());

		startSaleAs(registered.raceId(), SELLER_MEMBER_ID)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("작성 중인 대회만 판매를 시작할 수 있습니다."));

		assertThat(findRace(registered.raceId()).getSaleStatus()).isEqualTo(RaceSaleStatus.SCHEDULED);
	}

	@Test
	@DisplayName("작성 중에 취소한 대회의 판매를 시작하면 409이고 취소 상태 그대로다")
	void rejectStartSaleWhenCanceled() throws Exception {
		RegisteredRace registered = register(sameRace().build());
		postAs(RACES + registered.raceId() + "/cancel", SELLER_MEMBER_ID).andExpect(status().isOk());

		startSaleAs(registered.raceId(), SELLER_MEMBER_ID)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("작성 중인 대회만 판매를 시작할 수 있습니다."));

		assertThat(findRace(registered.raceId()).getSaleStatus()).isEqualTo(RaceSaleStatus.CANCELED);
	}

	@Test
	@DisplayName("다른 판매자(회원 9002)가 판매를 시작하면 403이고 작성 중 그대로다")
	void rejectStartSaleByOtherSeller() throws Exception {
		RegisteredRace registered = register(sameRace().build());

		startSaleAs(registered.raceId(), OTHER_SELLER_MEMBER_ID)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("본인이 등록한 대회만 처리할 수 있습니다."));

		assertThat(findRace(registered.raceId()).getSaleStatus()).isEqualTo(RaceSaleStatus.DRAFT);
	}

	@Test
	@DisplayName("판매자가 아닌 회원(9999)이 판매를 시작하면 403이다")
	void rejectStartSaleByNonSeller() throws Exception {
		RegisteredRace registered = register(sameRace().build());

		startSaleAs(registered.raceId(), NOT_SELLER_MEMBER_ID)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("승인된 판매자만 대회를 등록·수정할 수 있습니다."));

		assertThat(findRace(registered.raceId()).getSaleStatus()).isEqualTo(RaceSaleStatus.DRAFT);
	}

	@Test
	@DisplayName("없는 대회(ID 999999)의 판매를 시작하면 404이다")
	void rejectStartSaleOfMissingRace() throws Exception {
		startSaleAs(999_999, SELLER_MEMBER_ID)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("존재하지 않는 대회입니다."));
	}

	// ---------- 도우미 ----------

	// 기본 요청과 같은 일정 (대회 15일 뒤, 개인 예매 한 시간 전 오픈, 접수 마감 10일 뒤, 환불 기준일 = 대회 날짜)
	private RaceBuilder sameRace() {
		return race()
			.raceDate(now.plusDays(15))
			.individualOpenAt(now.minusHours(1))
			.applicationCloseAt(now.plusDays(10));
	}

	// 이미 끝난 크루 예매(3일 전 시작, 2일 전 마감)를 더한다. 개인 예매 오픈(한 시간 전)은 크루 마감 다음 날 06:00 이후다
	private RaceBuilder withPassedCrewSale(RaceBuilder builder) {
		return builder.crewSale(now.minusDays(3), now.minusDays(2));
	}

	// 등록한 기본 종목(10K)을 가리키는 수정용 종목. 바꿀 값만 고친다 (대표 이미지는 새 파일 ID로 바뀐다)
	private CourseBuilder sameCourse(int courseId) {
		return course().courseId(courseId);
	}

	private CourseBuilder halfCourse() {
		return course().name("하프").type(CourseType.HALF).distanceKm("21.1").timeLimitMinutes(180);
	}

	private CourseBuilder fiveKmCourse() {
		return course().name("5K").type(CourseType.KM_5).distanceKm("5").timeLimitMinutes(60);
	}

	private ResultActions update(int raceId, RaceSaveRequestDto request) throws Exception {
		return putJson(RACES + raceId, SELLER_MEMBER_ID, request);
	}

	private ResultActions startSaleAs(int raceId, Integer memberId) throws Exception {
		return postAs(RACES + raceId + "/sale", memberId);
	}

	private Race findRace(int raceId) {
		return raceRepository.findById(raceId).orElseThrow();
	}

	private RaceCourse findCourse(int courseId) {
		return raceCourseRepository.findById(courseId).orElseThrow();
	}

	private RaceCourseDocument findDocument(int courseId) {
		return raceCourseDocumentRepository.findById(String.valueOf(courseId)).orElseThrow();
	}

	private List<String> fileLinkMessages() {
		return sentMessages(KafkaTopics.RACE_FILE_LINK_REQUESTED);
	}

	private FileLinkRequestedEvent lastFileLink() {
		return jsonConverter.fromJson(fileLinkMessages().getLast(), FileLinkRequestedEvent.class);
	}

	private FileLinkRequestedEvent.Ref refOf(FileLinkRequestedEvent fileLink, int courseId) {
		return fileLink.refs().stream()
			.filter(ref -> ref.refId() == courseId)
			.findFirst()
			.orElseThrow();
	}
}
