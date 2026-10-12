package com.bukang.order.app;

import com.bukang.order.dto.CartItemDto;

// added가 false면 이미 담겨 있던 항목을 그대로 돌려준 것이다
public record CartAddResult(CartItemDto item, boolean added) {
}
