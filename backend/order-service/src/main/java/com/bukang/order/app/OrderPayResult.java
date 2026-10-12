package com.bukang.order.app;

import com.bukang.order.dto.OrderDto;

// alreadyPaid가 true면 이미 결제한 주문이라 아무것도 바꾸지 않고 그대로 돌려준 것이다
public record OrderPayResult(OrderDto order, boolean alreadyPaid) {
}
