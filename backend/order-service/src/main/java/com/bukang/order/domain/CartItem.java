package com.bukang.order.domain;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import com.bukang.common.global.jpa.entity.BaseIdAndTime;
import com.bukang.order.dto.CartItemDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 항목. 참가권(종목)만 담고 수량 칸은 두지 않는다 (ERD 최신 통합본 cart_item: item_type·quantity 뺌)
 * 종목은 상품 서비스(B) 소유라 FK 없이 ID 값만 둔다 (ERD 추천안 v4 설계 원칙)
 */
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@Table(
	name = "ORDER_CART_ITEM",
	uniqueConstraints = {
		// 같은 종목을 두 번 담지 못하게 해 수량 1을 지킨다. 동시에 담는 요청도 여기서 막힌다
		@UniqueConstraint(name = "uk_cart_item_course", columnNames = {"cart_id", "race_course_id"})
	}
)
public class CartItem extends BaseIdAndTime {
	@ManyToOne(fetch = LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@Column(name = "race_course_id", nullable = false)
	private int raceCourseId;

	CartItem(Cart cart, int raceCourseId) {
		this.cart = cart;
		this.raceCourseId = raceCourseId;
	}

	public CartItemDto toDto() {
		return new CartItemDto(getId(), getCreateDate(), raceCourseId);
	}
}
