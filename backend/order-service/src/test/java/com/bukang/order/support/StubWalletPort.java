package com.bukang.order.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.bukang.order.domain.exception.ExternalServiceUnavailableException;
import com.bukang.order.out.wallet.WalletPayResult;
import com.bukang.order.out.wallet.WalletPort;

/**
 * 테스트용 예치금. 잔액을 정할 수 있고(기본은 넉넉함), 같은 주문은 한 번만 차감한다 (D의 멱등 처리 흉내)
 * 결제를 되돌리면(cancelPayment) 차감한 금액을 잔액에 돌려준다. 같은 주문을 여러 번 되돌려도 한 번만 돌려준다
 * 환불(refund)은 그 주문에서 차감한 금액 전액을 주문마다 한 번만 돌려준다
 * 응답 시간 초과를 흉내 낼 수 있다: 결제는 차감까지 끝낸 뒤 예외를 던지고(D는 처리했지만 응답을 못 받음),
 * 결제 취소는 아무것도 하지 않고 예외를 던진다
 * 결제를 차감 직전에 멈춰 둘 수 있다 (holdPay): 결제가 주문을 잠근 채 D를 기다리는 순간에 다른 작업을 끼워 넣을 때 쓴다
 */
public class StubWalletPort implements WalletPort {
	private static final int DEFAULT_BALANCE = 1_000_000;
	private static final String TIMEOUT_MESSAGE = "예치금 서비스에 연결하지 못했습니다. 잠시 뒤 다시 시도해주세요.";

	private int balance = DEFAULT_BALANCE;
	// 차감한 채로 남아 있는 주문 ID → 차감 금액
	private final Map<Integer, Integer> paidOrders = new HashMap<>();
	private final List<Integer> payCalls = new CopyOnWriteArrayList<>();
	private final List<Integer> canceledOrders = new CopyOnWriteArrayList<>();
	private final List<Call> cancelCalls = new CopyOnWriteArrayList<>();
	private final List<Call> refundCalls = new CopyOnWriteArrayList<>();
	private final List<Integer> refundedAmounts = new CopyOnWriteArrayList<>();
	private boolean payTimesOut;
	private boolean cancelFails;
	private volatile CountDownLatch payEntered;
	private volatile CountDownLatch payReleased;

	// 예치금 호출 한 번: 어느 주문을 누구 지갑으로
	public record Call(int orderId, int memberId) {
	}

	// 멈춰 둔 결제는 모니터를 잡기 전에 기다린다. 그동안 결제 취소 같은 다른 호출이 막히지 않는다
	@Override
	public WalletPayResult pay(int orderId, int memberId, int amount) {
		CountDownLatch entered = payEntered;
		CountDownLatch released = payReleased;
		if (entered != null && released != null) {
			entered.countDown();
			await(released);
		}
		return payNow(orderId, amount);
	}

	private synchronized WalletPayResult payNow(int orderId, int amount) {
		payCalls.add(amount);
		WalletPayResult result = charge(orderId, amount);
		if (payTimesOut) {
			throw new ExternalServiceUnavailableException(TIMEOUT_MESSAGE);
		}
		return result;
	}

	private WalletPayResult charge(int orderId, int amount) {
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
	public synchronized void cancelPayment(int orderId, int memberId) {
		cancelCalls.add(new Call(orderId, memberId));
		if (cancelFails) {
			throw new ExternalServiceUnavailableException(TIMEOUT_MESSAGE);
		}
		canceledOrders.add(orderId);
		Integer amount = paidOrders.remove(orderId);
		if (amount != null) {
			balance += amount;
		}
	}

	@Override
	public synchronized void refund(int orderId, int memberId) {
		refundCalls.add(new Call(orderId, memberId));
		Integer amount = paidOrders.remove(orderId);
		if (amount != null) {
			refundedAmounts.add(amount);
			balance += amount;
		}
	}

	// 다음 결제를 차감 직전에 멈춘다. 돌려준 래치로 결제가 들어왔는지 기다리고, releasePay()로 이어 가게 한다
	public CountDownLatch holdPay() {
		payEntered = new CountDownLatch(1);
		payReleased = new CountDownLatch(1);
		return payEntered;
	}

	public void releasePay() {
		CountDownLatch released = payReleased;
		if (released != null) {
			released.countDown();
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) {
				throw new IllegalStateException("멈춰 둔 결제가 30초 안에 풀리지 않았습니다.");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	// 결제 응답 시간 초과를 흉내 낸다. 차감은 되고 주문 쪽은 예외를 받는다
	public synchronized void setPayTimesOut(boolean payTimesOut) {
		this.payTimesOut = payTimesOut;
	}

	// 결제 취소가 실패하게 한다 (예치금 서비스 장애)
	public synchronized void setCancelFails(boolean cancelFails) {
		this.cancelFails = cancelFails;
	}

	public List<Call> cancelCalls() {
		return cancelCalls;
	}

	public List<Call> refundCalls() {
		return refundCalls;
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

	// 차감한 채로 남아 있는 주문 (결제를 되돌리거나 환불한 주문은 빠진다)
	public synchronized Set<Integer> chargedOrders() {
		return Set.copyOf(paidOrders.keySet());
	}

	// 결제 취소가 성공한 주문
	public List<Integer> canceledOrders() {
		return canceledOrders;
	}

	public synchronized void clear() {
		balance = DEFAULT_BALANCE;
		paidOrders.clear();
		payCalls.clear();
		canceledOrders.clear();
		cancelCalls.clear();
		refundCalls.clear();
		refundedAmounts.clear();
		payTimesOut = false;
		cancelFails = false;
		releasePay();
		payEntered = null;
		payReleased = null;
	}
}
