package com.bukang.product.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.product.domain.exception.RaceChangeNotAllowedException;
import com.bukang.product.dto.RaceCourseSaveRequestDto;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대회 종목 (참가권). 컬럼은 크루 예매 ERD v5의 race_course 표에 피그잼의 나이 제한·키트 포함을 더했다.
 * 크루 몫·개인 몫 정원과 확보·판매 수를 센다. 남은 자리 판정은 이 행 하나의 조건부 UPDATE로 한다.
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@Table(name = "race_course")
@AttributeOverride(name = "id", column = @Column(name = "race_course_id"))
@AttributeOverride(name = "createDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "modifyDate", column = @Column(name = "updated_at", nullable = false))
public class RaceCourse extends BaseIdAndTime {
	@ManyToOne(fetch = LAZY, optional = false)
	@JoinColumn(name = "race_id", nullable = false)
	private Race race;

	// 종목명 (10K, 하프 등)
	@Column(nullable = false, length = 50)
	private String name;

	@Enumerated(STRING)
	@Column(name = "course_type", nullable = false, columnDefinition = "varchar(10)")
	private CourseType courseType;

	@Enumerated(STRING)
	@Column(name = "surface", nullable = false, columnDefinition = "varchar(10)")
	private CourseSurface surface;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal distanceKm;

	// 제한 시간(분). 검색용 필요 페이스 = 제한 시간 ÷ 거리
	@Column(nullable = false)
	private int timeLimitMinutes;

	// 참가 나이 (대회 당일 만 나이). 최소는 만 14세보다 낮게 정할 수 없고, 최대가 null이면 상한이 없다
	@Column(nullable = false)
	private int minAge;

	private Integer maxAge;

	// 참가비 (판매 시작 뒤에는 구매자에게 유리한 변경만)
	@Column(nullable = false)
	private int price;

	// 참가권에 포함된 키트 금액 (참가권과 함께 판매·취소하고 배송으로만 받는다)
	@ColumnDefault("0")
	@Column(nullable = false)
	private int kitPrice;

	@Column(nullable = false)
	private boolean kitIncluded;

	// 정원과 확보·판매 수. 카운터는 조건부 UPDATE로만 바꾼다
	// 남은 크루 자리 = crewQuota − movedQuota − crewReserved − crewSold
	// 남은 개인 자리 = individualQuota + movedQuota − individualReserved − individualSold
	@ColumnDefault("0")
	@Column(nullable = false)
	private int crewQuota;

	@ColumnDefault("0")
	@Column(nullable = false)
	private int individualQuota;

	@ColumnDefault("0")
	@Column(nullable = false)
	private int crewReserved;

	@ColumnDefault("0")
	@Column(nullable = false)
	private int individualReserved;

	@ColumnDefault("0")
	@Column(nullable = false)
	private int crewSold;

	@ColumnDefault("0")
	@Column(nullable = false)
	private int individualSold;

	// 크루 기간이 끝나 개인 몫으로 옮긴 크루 자리 수. 늘기만 한다
	@ColumnDefault("0")
	@Column(nullable = false)
	private int movedQuota;

	// 크루 몫을 개인 몫으로 옮긴 일시. null이면 크루 기간 진행 중 (두 번 옮기지 않는 조건)
	private LocalDateTime crewClosedAt;

	@Enumerated(STRING)
	@ColumnDefault("'OPEN'")
	@Column(name = "status", nullable = false, columnDefinition = "varchar(20)")
	private CourseStatus status;

	public RaceCourse(RaceCourseSaveRequestDto request) {
		this.status = CourseStatus.OPEN;
		apply(request);
	}

	public void update(RaceCourseSaveRequestDto request, boolean saleStarted) {
		// 판매 시작 뒤에는 구매자에게 유리한 변경만 된다 (ERD price 주석). 키트 금액은 참가비에 포함된 금액이다
		if (saleStarted && request.getPrice() > price) {
			throw new RaceChangeNotAllowedException(withName("판매가 시작된 종목은 참가비를 올릴 수 없습니다."));
		}
		// 정원은 확보·판매된 자리보다 줄이지 못한다. 몫별로 남은 자리 식이 0 이상이어야 한다 (크루 예매 ERD v5)
		//   개인: individualQuota + movedQuota >= individualReserved + individualSold
		//   크루: crewQuota − movedQuota >= crewReserved + crewSold
		if (request.getIndividualQuota() + movedQuota < individualReserved + individualSold
			|| request.getCrewQuota() - movedQuota < crewReserved + crewSold) {
			throw new RaceChangeNotAllowedException(withName("정원을 확보·판매된 자리 수보다 줄일 수 없습니다."));
		}
		apply(request);
	}

	// 확보·판매된 자리가 있으면 구매자가 있으므로 종목을 뺄 수 없다
	public void checkRemovable() {
		if (crewReserved + crewSold + individualReserved + individualSold > 0) {
			throw new RaceChangeNotAllowedException(withName("확보·판매된 자리가 있는 종목은 뺄 수 없습니다."));
		}
	}

	private String withName(String message) {
		return "%s (종목: %s)".formatted(message, name);
	}

	// 판매자가 입력하는 칸을 요청 값으로 채운다. 확보·판매 수와 상태는 정원 변경 API(⑧-3)가 바꾼다
	private void apply(RaceCourseSaveRequestDto request) {
		this.name = request.getName();
		this.courseType = request.getCourseType();
		this.surface = request.getSurface();
		this.distanceKm = request.getDistanceKm();
		this.timeLimitMinutes = request.getTimeLimitMinutes();
		this.minAge = request.getMinAge();
		this.maxAge = request.getMaxAge();
		this.price = request.getPrice();
		this.kitPrice = request.getKitPrice();
		this.kitIncluded = request.getKitIncluded();
		this.crewQuota = request.getCrewQuota();
		this.individualQuota = request.getIndividualQuota();
	}

	public void addRace(Race race) {
		this.race = race;
	}
}
