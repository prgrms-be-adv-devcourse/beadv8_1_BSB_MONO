package com.bukang.order.out.race;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * 참가 신청에 필요한 종목(참가권) 정보. B(상품)의 race · race_course에서 필요한 값만 옮긴 것이다
 * (ERD 추천안 v4 RACE_RACE · RACE_COURSE)
 *
 * @param raceCourseId       종목 ID
 * @param raceId             대회 ID
 * @param raceDate           대회 일시. 나이는 이날의 만 나이로 따진다 (정책 2장 1-9)
 * @param applicationOpenAt  개인 예매 오픈 일시 (individual_open_at)
 * @param applicationCloseAt 접수 마감 일시 = 참가자 정보 수정 기한 (정책 2장 1-6 · 3장 1-5)
 * @param minAge             참가 가능 최소 나이 (만 14세 이상)
 * @param maxAge             참가 가능 최대 나이. null이면 제한 없음
 */
public record RaceCourseInfo(
	int raceCourseId,
	int raceId,
	LocalDateTime raceDate,
	LocalDateTime applicationOpenAt,
	LocalDateTime applicationCloseAt,
	int minAge,
	Integer maxAge
) {
	// 접수 기간 안인지 (오픈 시각 이상, 마감 시각 전)
	public boolean isAccepting(LocalDateTime now) {
		return !now.isBefore(applicationOpenAt) && now.isBefore(applicationCloseAt);
	}

	// 접수 마감 전인지 (참가자 정보 수정 가능 여부)
	public boolean isBeforeClose(LocalDateTime now) {
		return now.isBefore(applicationCloseAt);
	}

	// 대회 당일 만 나이가 종목의 나이 제한 안인지
	public boolean allowsAge(LocalDate birth) {
		int age = ageOnRaceDay(birth);
		return age >= minAge && (maxAge == null || age <= maxAge);
	}

	public int ageOnRaceDay(LocalDate birth) {
		return Period.between(birth, raceDate.toLocalDate()).getYears();
	}

	// 사용자에게 보여 줄 나이 제한 문구 (예: "만 14세 이상", "만 20세 ~ 만 60세")
	public String ageRangeLabel() {
		if (maxAge == null) {
			return "만 %d세 이상".formatted(minAge);
		}
		return "만 %d세 ~ 만 %d세".formatted(minAge, maxAge);
	}
}
