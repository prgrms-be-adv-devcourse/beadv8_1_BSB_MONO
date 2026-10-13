package com.bukang.order.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bukang.order.out.wallet.WalletPayResult;
import com.bukang.order.out.wallet.WalletPort;

/**
 * 테스트용 예치금. 잔액을 정할 수 있고(기본은 넉넉함), 같은 주문은 한 번만 차감한다 (D의 멱등 처리 흉내)
 * 결제를 되돌리면(cancelPayment) 차감한 금액을 잔액에 돌려준다. 같은 주문을 여러 번 되돌려도 한 번만 돌려준다
 */
public class StubWalletPort implements WalletPort {
	private static final int DEFAULT_BALANCE = 1_000_000;

	private int balance = DEFAULT_BALANCE;
	// 차감한 채로 남아 있는 주문 ID → 차감 금액
	private final Map<Integer, Integer> paidOrders = new HashMap<>();
	private final List<Integer> payCalls = new CopyOnWriteArrayList<>();
	private final List<Integer> canceledOrders = new CopyOnWriteArrayList<>();
	private final List<Integer> refundedOrderItems = new CopyOnWriteArrayList<>();
	private final List<Integer> refundedAmounts = new CopyOnWriteArrayList<>();

	@Override
	public synchronized WalletPayResult pay(int orderId, int memberId, int amount) {
		payCalls.add(amount);
		if (paidOrders.containsKey(orderId)) {
			return WalletPayResult.paid();
		}
		if (balance < amount) {
			return WalletPayResult.insufficient(amount - balance);
		}
		balance -= amount;
		paidOrders.put(orderId, amount);
		return WalletPayResult.paid();
	}

	@Override
	public synchronized void cancelPayment(int orderId) {
		canceledOrders.add(orderId);
		Integer amount = paidOrders.remove(orderId);
		if (amount != null) {
			balance += amount;
		}
	}

	@Override
	public synchronized void refund(int orderId, int orderItemId, int amount) {
		refundedOrderItems.add(orderItemId);
		refundedAmounts.add(amount);
		balance += amount;
	}

	public List<Integer> refundedOrderItems() {
		return refundedOrderItems;
	}

	public List<Integer> refundedAmounts() {
		return refundedAmounts;
	}

	public synchronized void setBalance(int balance) {
		this.balance = balance;
	}

	public synchronized int balance() {
		return balance;
	}

	public List<Integer> payCalls() {
		return payCalls;
	}

	// 차감한 채로 남아 있는 주문 (결제를 되돌린 주문은 빠진다)
	public synchronized Set<Integer> chargedOrders() {
		return Set.copyOf(paidOrders.keySet());
	}

	public List<Integer> canceledOrders() {
		return canceledOrders;
	}

	public synchronized void clear() {
		balance = DEFAULT_BALANCE;
		paidOrders.clear();
		payCalls.clear();
		canceledOrders.clear();
		refundedOrderItems.clear();
		refundedAmounts.clear();
	}
}
