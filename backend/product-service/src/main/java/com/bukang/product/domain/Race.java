package com.bukang.product.domain;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.product.domain.exception.RaceChangeNotAllowedException;
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
 * 종목(참가권)과 정원, 이미지는 RaceCourse에 있다. 대회에 이미지를 붙일지는 최종 프로젝트에서 정한다.
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@Table(name = "race")
// 바뀐 컬럼만 UPDATE한다. 판매자가 수정하는 동안 상태 배치(RaceSaleStatusScheduler)가 바꾼 판매 상태를
// 수정 트랜잭션이 처음 읽은 옛 상태로 덮어쓰지 않게 하려는 것이다 (수정은 판매 상태를 바꾸지 않는다)
@DynamicUpdate
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

	// 일정·장소 변경 안내 일시 (판매 시작 뒤 대회 일시나 장소를 바꾼 일시. 이날부터 7일 동안은 환불 기준일과 상관없이 전액 환불)
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

	public void update(RaceSaveRequestDto request, boolean saleStarted, LocalDateTime now) {
		// 판매 시작 뒤에는 환불 기준일을 늦추는 변경만 된다 (구매자에게 유리한 변경만, ERD refund_deadline 주석)
		if (saleStarted && request.getRefundDeadline().isBefore(refundDeadline)) {
			throw new RaceChangeNotAllowedException("판매가 시작된 대회는 환불 기준일을 앞당길 수 없습니다.");
		}
		// 판매 시작을 누른 뒤에는 판매 일정을 마음대로 바꾸지 못한다 (대기열, 크루 마감 다음 날 06:00 배치, 확보 기한이 이 일정을 따른다)
		// 판매 시작 일시는 크루 예매 시작·개인 예매 오픈으로 계산하므로, 이 검사로 판매 시작 일시도 지난 뒤에는 바뀌지 않는다
		if (saleStatus != RaceSaleStatus.DRAFT) {
			checkScheduleChange("크루 예매 시작", crewSaleStartAt, request.getCrewSaleStartAt(), now);
			checkScheduleChange("크루 예매 마감", crewSaleEndAt, request.getCrewSaleEndAt(), now);
			checkScheduleChange("개인 예매 오픈", individualOpenAt, request.getIndividualOpenAt(), now);
			checkScheduleChange("접수 마감", applicationCloseAt, request.getApplicationCloseAt(), now);
		}
		// 판매 시작 뒤 대회 일시(앞당기기·늦추기 모두)나 장소가 바뀌면 일정·장소 변경 안내 일시를 기록한다
		// 이날부터 7일 동안은 환불 기준일과 상관없이 전액 환불된다 (ERD schedule_changed_at, 환불 판정은 주문 쪽)
		// 장소도 참가 여부를 다시 생각하게 하는 변경이라 대회 일시와 같은 기준으로 환불한다
		// 다시 바꾸면 마지막으로 바꾼 일시로 다시 7일을 센다
		// 환불 기준일은 앞당길 수 없고 대회 날짜보다 늦을 수 없으므로, 대회일은 환불 기준일보다 앞으로는 당길 수 없다
		if (saleStarted && (!request.getRaceDate().equals(raceDate) || isPlaceChanged(request))) {
			this.scheduleChangedAt = now;
		}
		apply(request);
	}

	// 장소는 화면에 보이는 주소와 검색 필터(시·도, 시·군·구) 중 하나라도 바뀌면 바뀐 것으로 본다
	private boolean isPlaceChanged(RaceSaveRequestDto request) {
		return !Objects.equals(location, request.getLocation())
			|| !Objects.equals(regionSido, request.getRegionSido())
			|| !Objects.equals(regionSigungu, request.getRegionSigungu());
	}

	// 이미 지난 일정은 바꿀 수 없고, 아직 오지 않은 일정도 지난 시각으로 옮길 수 없다
	//   예: 지금 10/16, 크루 예매 시작 10/13(지남) → 바꿀 수 없음 / 개인 예매 오픈 10/20 → 10/22로는 되고 10/15로는 안 됨
	private static void checkScheduleChange(String name, LocalDateTime current, LocalDateTime requested,
		LocalDateTime now) {
		if (Objects.equals(current, requested)) {
			return;
		}
		if (current != null && !current.isAfter(now)) {
			throw new RaceChangeNotAllowedException("이미 지난 %s 일시는 바꿀 수 없습니다.".formatted(name));
		}
		if (requested != null && !requested.isAfter(now)) {
			throw new RaceChangeNotAllowedException("%s 일시를 지난 시각으로 바꿀 수 없습니다.".formatted(name));
		}
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

	// 목록에서 빼면 orphanRemoval로 종목 행이 지워진다
	public void removeRaceCourse(RaceCourse raceCourse) {
		courses.remove(raceCourse);
	}

	// 종목 목록을 요청 순서에 맞춘다 (응답의 종목 ID 순서). 순서 컬럼이 없어 DB에는 영향이 없다
	public void sortCourses(List<RaceCourse> requestOrder) {
		courses.sort(Comparator.comparingInt(requestOrder::indexOf));
	}

	public boolean isOwnedBy(ProductSeller seller) {
		return this.seller.getId() == seller.getId();
	}

	// 작성 중인 대회는 등록한 판매자(회원)만 볼 수 있고, 판매를 시작한 대회는 누구나 볼 수 있다
	public boolean isVisibleTo(Integer memberId) {
		return saleStatus != RaceSaleStatus.DRAFT || (memberId != null && seller.getMember().getId() == memberId);
	}

	// 판매자가 판매 시작을 누르면 판매 예정이 되고, 판매 시작 일시가 이미 지났으면 바로 판매 중이 된다
	// 대회 검수는 없다 (ERD race.sale_status 주석: 판매자가 직접 판매 시작)
	public void startSale(LocalDateTime now) {
		if (saleStatus != RaceSaleStatus.DRAFT) {
			throw new RaceChangeNotAllowedException("작성 중인 대회만 판매를 시작할 수 있습니다.");
		}
		this.saleStatus = now.isBefore(saleOpenAt) ? RaceSaleStatus.SCHEDULED : RaceSaleStatus.ON_SALE;
	}

	// 판매자가 판매를 시작했고(작성 중이 아님) 판매 시작 일시가 지났으면, 구매자가 있을 수 있는 대회다
	public boolean isSaleStarted(LocalDateTime now) {
		return saleStatus != RaceSaleStatus.DRAFT && !now.isBefore(saleOpenAt);
	}

	// 개인 예매로 자리를 확보할 수 있는지: 판매 예정·판매 중이고, 개인 예매 오픈부터 접수 마감 전까지
	// 판매 예정도 넣는 이유: 판매 시작 일시가 지나도 상태 배치가 판매 중으로 바꾸기까지 최대 5분 걸린다
	public boolean isAcceptingIndividual(LocalDateTime now) {
		return (saleStatus == RaceSaleStatus.SCHEDULED || saleStatus == RaceSaleStatus.ON_SALE)
			&& !now.isBefore(individualOpenAt) && now.isBefore(applicationCloseAt);
	}
}
