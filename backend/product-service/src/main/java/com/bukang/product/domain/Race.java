package com.bukang.product.domain;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.product.dto.RaceSaveRequestDto;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 판매자가 등록한 대회 (상품). 컬럼은 크루 예매 ERD v5의 race 표를 따른다.
 * 종목(참가권)과 정원은 RaceCourse에 있다. 클래스 이름 Race가 파일 연결 대상 종류(FileRef refType)가 된다.
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@Table(name = "race")
@AttributeOverride(name = "id", column = @Column(name = "race_id"))
@AttributeOverride(name = "createDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "modifyDate", column = @Column(name = "updated_at", nullable = false))
public class Race extends BaseIdAndTime {
	@ManyToOne(fetch = LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false)
	private ProductSeller seller;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private String location;

	// 검색 필터. 등록 때 주소 검색 API로 채운다 (시·군·구는 이름이 겹쳐 시·도와 함께 쓴다)
	@Column(nullable = false, length = 20)
	private String regionSido;

	@Column(nullable = false, length = 30)
	private String regionSigungu;

	@Column(nullable = false)
	private LocalDateTime raceDate;

	// 판매 시작 일시. 판매자가 입력하지 않고 크루 예매 시작·개인 예매 오픈 중 이른 시각으로 채운다
	@Column(nullable = false)
	private LocalDateTime saleOpenAt;

	// 크루 예매 기간. 시작 일시가 null이면 크루 모집 없이 개인 예매만 한다
	private LocalDateTime crewSaleStartAt;

	private LocalDateTime crewSaleEndAt;

	@Column(nullable = false)
	private LocalDateTime individualOpenAt;

	// 접수 마감 일시 (참가자 정보 수정 기한)
	@Column(nullable = false)
	private LocalDateTime applicationCloseAt;

	// 환불 판정 기준일: 이날까지 취소하면 전액 환불, 지나면 환불 불가
	@Column(nullable = false)
	private LocalDate refundDeadline;

	// 화면에 보여 주는 취소·환불 안내 글 (판정은 refundDeadline으로 한다)
	@Column(columnDefinition = "TEXT")
	private String refundPolicy;

	// 크루 참가 신청 인원. 최소는 5 이상만 정할 수 있고, 최대가 null이면 상한이 없다
	@ColumnDefault("5")
	@Column(nullable = false)
	private int crewMinCount;

	private Integer crewMaxCount;

	@Enumerated(STRING)
	@ColumnDefault("'DRAFT'")
	@Column(name = "sale_status", nullable = false, columnDefinition = "varchar(20)")
	private RaceSaleStatus saleStatus;

	// 일정 변경 안내 일시 (이날부터 7일 동안은 환불 기준일과 상관없이 전액 환불)
	private LocalDateTime scheduleChangedAt;

	private LocalDateTime canceledAt;

	@OneToMany(mappedBy = "race", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
	private List<RaceCourse> courses = new ArrayList<>();

	public Race(ProductSeller seller, RaceSaveRequestDto request) {
		this.seller = seller;
		// 등록하면 작성 중이다. 판매자가 판매 시작을 눌러야 판매 예정이 된다
		this.saleStatus = RaceSaleStatus.DRAFT;
		apply(request);
	}

	// 판매자가 입력하는 칸을 요청 값으로 채운다
	private void apply(RaceSaveRequestDto request) {
		this.title = request.getTitle();
		this.description = request.getDescription();
		this.location = request.getLocation();
		this.regionSido = request.getRegionSido();
		this.regionSigungu = request.getRegionSigungu();
		this.raceDate = request.getRaceDate();
		this.crewSaleStartAt = request.getCrewSaleStartAt();
		this.crewSaleEndAt = request.getCrewSaleEndAt();
		this.individualOpenAt = request.getIndividualOpenAt();
		this.applicationCloseAt = request.getApplicationCloseAt();
		this.refundDeadline = request.getRefundDeadline();
		this.refundPolicy = request.getRefundPolicy();
		this.crewMinCount = request.getCrewMinCount();
		this.crewMaxCount = request.getCrewMaxCount();
		this.saleOpenAt = earliestOpenAt(request.getCrewSaleStartAt(), request.getIndividualOpenAt());
	}

	// 크루 예매가 없으면(시작 일시 null) 개인 예매 오픈이 판매 시작이다
	private static LocalDateTime earliestOpenAt(LocalDateTime crewSaleStartAt, LocalDateTime individualOpenAt) {
		if (crewSaleStartAt != null && crewSaleStartAt.isBefore(individualOpenAt)) {
			return crewSaleStartAt;
		}
		return individualOpenAt;
	}

	public void addRaceCourse(RaceCourse raceCourse) {
		courses.add(raceCourse);
		raceCourse.addRace(this);
	}
}
