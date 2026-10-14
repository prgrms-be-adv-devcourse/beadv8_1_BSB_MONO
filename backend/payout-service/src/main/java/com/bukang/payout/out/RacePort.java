package com.bukang.payout.out;

import java.util.Optional;

/**
 * 정산이 상품 서비스(B)에서 받아야 하는 대회 정보의 계약 (포트)
 * 정산 내역을 만들 때 한 번 물어 이름을 정산 내역에 저장한다. 판매자 조회 때는 부르지 않으므로,
 * 상품 서비스가 멈춰도 정산 화면은 뜬다
 * 상품 서비스가 생기면 이 인터페이스를 구현한 어댑터(HTTP 호출)를 붙인다. 배치 코드는 바뀌지 않는다
 */
@FunctionalInterface
public interface RacePort {
	/**
	 * 대회 ID로 대회 이름을 찾는다. 없으면 빈 값을 돌려준다
	 */
	Optional<String> findRaceName(int raceId);
}
