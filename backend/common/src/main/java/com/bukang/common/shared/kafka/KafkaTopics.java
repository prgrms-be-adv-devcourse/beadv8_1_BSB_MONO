package com.bukang.common.shared.kafka;

/**
 * Kafka 토픽 이름을 모두 이곳에 모은다 (생산자와 소비자가 같은 상수를 쓴다)
 * 토픽 이름 값은 서비스마다 쓰던 그대로 두었다. 형식이 두 가지라 회원 쪽과 맞출 때 하나로 정한다.
 * - queuing.<도메인>.<이벤트>: 한 토픽을 여러 서비스가 groupId를 달리해 구독한다 (회원, 캐시)
 * - <받는 서비스>.<도메인>.<이벤트>: 같은 이벤트도 받는 서비스마다 토픽을 나눈다 (상품)
 */
public final class KafkaTopics {
	// 회원 이벤트 (member-service가 발행하고 다른 서비스가 구독한다)
	public static final String MEMBER_MEMBER_JOINED = "queuing.member.joined";
	public static final String MEMBER_MODIFIED = "queuing.member.modified";

	// 캐시 회원 복제본을 처음 저장한 뒤 지갑을 만든다 (cash → cash)
	public static final String CASH_MEMBER_CREATED = "queuing.cashmember.created";

	public static final String PRODUCT_MEMBER_JOINED = "product.member.joined";
	public static final String PRODUCT_SELLER_CHANGED = "product.seller.changed";
	// 대회·종목을 저장한 뒤 이미지 연결을 요청한다 (product → file)
	public static final String RACE_FILE_LINK_REQUESTED = "race.filelink.requested";

	private KafkaTopics() {
	}
}
