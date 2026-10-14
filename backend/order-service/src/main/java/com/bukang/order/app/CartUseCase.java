package com.bukang.order.app;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.bukang.order.domain.Cart;
import com.bukang.order.domain.CartItem;
import com.bukang.order.domain.exception.ApplicationPeriodException;
import com.bukang.order.domain.exception.CartConflictException;
import com.bukang.order.domain.exception.CartItemAccessDeniedException;
import com.bukang.order.domain.exception.CartItemNotFoundException;
import com.bukang.order.domain.exception.RaceCourseNotFoundException;
import com.bukang.order.out.CartItemRepository;
import com.bukang.order.out.CartRepository;
import com.bukang.order.out.race.RaceCourseInfo;
import com.bukang.order.out.race.RaceCoursePort;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니에 참가권을 담고 뺀다 (정책 4장 1-1). 담기만으로는 자리를 확보하지 않으므로 상품 서비스(B)에는 종목 조회만 한다
 */
@Service
@RequiredArgsConstructor
public class CartUseCase {
	// Cart·CartItem의 @UniqueConstraint 이름과 같아야 한다
	private static final List<String> CART_CONSTRAINTS = List.of("uk_cart_member", "uk_cart_item_course");

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final RaceCoursePort raceCoursePort;

	public CartAddResult addItem(int memberId, int raceCourseId, LocalDateTime now) {
		RaceCourseInfo course = raceCoursePort.findCourse(raceCourseId)
			.orElseThrow(() -> new RaceCourseNotFoundException("존재하지 않는 종목입니다."));
		ParticipationApplyUseCase.checkNotCanceled(course);
		// 접수가 끝난 종목은 결제할 수 없으니 담지도 않는다. 접수 시작 전 종목은 미리 담아 둘 수 있다
		if (!course.isBeforeClose(now)) {
			throw new ApplicationPeriodException("접수가 마감된 종목은 담을 수 없습니다.");
		}
		try {
			Cart cart = cartRepository.findByMemberId(memberId)
				.orElseGet(() -> cartRepository.save(Cart.of(memberId)));
			boolean added = cart.findItem(raceCourseId).isEmpty();
			CartItem item = cart.addItem(raceCourseId);
			// 새 항목의 ID를 받고, 유니크 제약 위반을 여기서 잡으려고 바로 반영한다
			cartRepository.flush();
			return new CartAddResult(item.toDto(), added);
		} catch (DataIntegrityViolationException exception) {
			// 같은 회원의 첫 담기, 같은 종목 담기가 동시에 들어오면 늦은 쪽이 유니크 제약에 걸린다.
			// 한 트랜잭션 안에서는 다시 읽어 이어 갈 수 없으므로 다시 시도하라고 알린다
			if (isCartConflict(exception)) {
				throw new CartConflictException("장바구니에 담는 요청이 겹쳤습니다. 다시 시도해주세요.");
			}
			throw exception;
		}
	}

	public Cart removeItem(int memberId, int cartItemId) {
		CartItem item = cartItemRepository.findById(cartItemId)
			.orElseThrow(() -> new CartItemNotFoundException("장바구니에 없는 항목입니다."));
		Cart cart = item.getCart();
		if (!cart.isOwnedBy(memberId)) {
			throw new CartItemAccessDeniedException("본인의 장바구니 항목만 뺄 수 있습니다.");
		}
		cart.removeItem(item);
		return cart;
	}

	private boolean isCartConflict(DataIntegrityViolationException exception) {
		String message = exception.getMostSpecificCause().getMessage();
		if (message == null) {
			return false;
		}
		String lowerCaseMessage = message.toLowerCase();
		return CART_CONSTRAINTS.stream().anyMatch(lowerCaseMessage::contains);
	}
}
