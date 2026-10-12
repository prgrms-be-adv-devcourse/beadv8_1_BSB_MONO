package com.bukang.order.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.order.domain.Cart;
import com.bukang.order.dto.CartDto;
import com.bukang.order.dto.CartItemAddRequestDto;
import com.bukang.order.out.CartRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CartFacade {
	private final CartUseCase cartUseCase;
	private final CartRepository cartRepository;

	// 장바구니를 아직 만들지 않은 회원은 빈 장바구니로 보여 준다 (조회만으로는 만들지 않는다)
	public CartDto findCart(int memberId) {
		return cartRepository.findByMemberId(memberId)
			.map(Cart::toDto)
			.orElseGet(() -> new CartDto(memberId, List.of()));
	}

	@Transactional
	public CartAddResult addItem(CartItemAddRequestDto request, int memberId) {
		return cartUseCase.addItem(memberId, request.getRaceCourseId(), LocalDateTime.now());
	}

	@Transactional
	public CartDto removeItem(int cartItemId, int memberId) {
		return cartUseCase.removeItem(memberId, cartItemId).toDto();
	}
}
