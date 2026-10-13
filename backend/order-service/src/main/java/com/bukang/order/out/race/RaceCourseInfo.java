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
 * @param sellerId           판매자 ID (정산이 주문 상품에서 읽는다)
 * @param courseName         주문 상품명으로 남길 이름 (예: "2026 서울 하프 마라톤 10K")
 * @param price              참가비 (원)
 * @param kitPrice           참가권에 포함된 키트 금액 (원). 키트가 없으면 0
 * @param kitIncluded        키트가 있는 종목인지 (ERD v5 race_course.kit_included). 키트는 배송으로만 받으므로
 *                           있으면 신청 때 배송 주소를 받는다 (정책 2장 1-3, 3장 1-2)
 * @param refundDeadline     환불 기한 (날짜). 이날 23:59:59까지 취소하면 전액 환불한다 (정책 5장, ERD v4 race.refund_deadline)
 * @param scheduleChangedAt  일정 변경 안내 일시. 판매 시작 뒤 대회 일시가 바뀌면 상품 서비스가 기록한다. 바뀐 적 없으면 null
 *                           (ERD v4 race.schedule_changed_at)
 */
public record RaceCourseInfo(
	int raceCourseId,
	int raceId,
	LocalDateTime raceDate,
	LocalDateTime applicationOpenAt,
	LocalDateTime applicationCloseAt,
	int minAge,
	Integer maxAge,
	int sellerId,
	String courseName,
	int price,
	int kitPrice,
	boolean kitIncluded,
	LocalDate refundDeadline,
	LocalDateTime scheduleChangedAt
) {
	// 일정이 바뀐 뒤 전액 환불해 주는 날 수 (정책 5장 4-4 "알린 날부터 7일 동안", ERD v4 race.schedule_changed_at)
	public static final int SCHEDULE_CHANGE_REFUND_DAYS = 7;

	/**
	 * 전액 환불로 취소할 수 있는 날인지 (ERD v4 환불 판정)
	 * 환불 기한 날짜까지(그날 23:59:59 포함, 10/12 결정)이거나, 일정이 바뀐 뒤 7일 안이면 기한과 상관없이 된다
	 */
	public boolean isRefundableOn(LocalDate today) {
		return !today.isAfter(refundDeadline) || isWithinScheduleChangeRefund(today);
	}

	/**
	 * 일정 변경 뒤 전액 환불 기간 안인지. 환불 기한과 같이 날짜 단위로 센다
	 * 정책 5장 4-4 "알린 날부터 7일 동안"대로 알린 날(일정 변경 일시)을 1일째로 센다: 10/13 14:00에 바뀌면 10/19 23:59:59까지다
	 */
	public boolean isWithinScheduleChangeRefund(LocalDate today) {
		LocalDate lastDay = scheduleChangeRefundLastDay();
		return lastDay != null && !today.isAfter(lastDay);
	}

	// 일정 변경 뒤 전액 환불 기간의 마지막 날 (그날 23:59:59까지). 일정이 바뀐 적 없으면 null
	public LocalDate scheduleChangeRefundLastDay() {
		if (scheduleChangedAt == null) {
			return null;
		}
		return scheduleChangedAt.toLocalDate().plusDays(SCHEDULE_CHANGE_REFUND_DAYS - 1);
	}

	// 구매자가 내는 참가권 한 장 값. 키트는 참가권과 함께 팔고 함께 취소한다 (ERD race_course.kit_price)
	public int ticketPrice() {
		return price + kitPrice;
	}

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
