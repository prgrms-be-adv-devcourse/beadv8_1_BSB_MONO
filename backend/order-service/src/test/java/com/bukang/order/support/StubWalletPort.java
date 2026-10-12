package com.bukang.order.support;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bukang.order.out.wallet.WalletPayResult;
import com.bukang.order.out.wallet.WalletPort;

/**
 * 테스트용 예치금. 잔액을 정할 수 있고(기본은 넉넉함), 같은 주문은 한 번만 차감한다 (D의 멱등 처리 흉내)
 */
public class StubWalletPort implements WalletPort {
	private static final int DEFAULT_BALANCE = 1_000_000;

	private int balance = DEFAULT_BALANCE;
	private final Set<Integer> paidOrders = new HashSet<>();
	private final List<Integer> payCalls = new CopyOnWriteArrayList<>();
	private final List<Integer> canceledOrders = new CopyOnWriteArrayList<>();

	@Override
	public synchronized WalletPayResult pay(int orderId, int memberId, int amount) {
		payCalls.add(amount);
		if (paidOrders.contains(orderId)) {
			return WalletPayResult.paid();
		}
		if (balance < amount) {
			return WalletPayResult.insufficient(amount - balance);
		}
		balance -= amount;
		paidOrders.add(orderId);
		return WalletPayResult.paid();
	}

	@Override
	public synchronized void cancelPayment(int orderId) {
		canceledOrders.add(orderId);
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

	public List<Integer> canceledOrders() {
		return canceledOrders;
	}

	public synchronized void clear() {
		balance = DEFAULT_BALANCE;
		paidOrders.clear();
		payCalls.clear();
		canceledOrders.clear();
	}
}
