package com.bukang.member.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import com.bukang.common.global.json.JsonConverter;
import com.bukang.common.shared.kafka.KafkaTopics;
import com.bukang.common.shared.member.event.MemberJoinedEvent;
import com.bukang.member.app.MemberFacade;
import com.bukang.member.app.MemberJoinUseCase;
import com.bukang.member.domain.TermsType;
import com.bukang.member.domain.exception.DuplicateEmailException;
import com.bukang.member.dto.MemberDto;
import com.bukang.member.dto.MemberJoinRequestDto;

// 실제 임베디드 Kafka 브로커로 가입 이벤트가 커밋 뒤에만 queuing.member.joined에 나가는지 확인한다.
// - 다른 테스트의 공유 H2(bukang-test)와 분리된 전용 DB를 쓴다 (create-drop이 다른 테스트 데이터를 건드리지 않게)
// - 테스트마다 새 컨슈머 그룹으로 토픽을 처음부터 읽으므로, 메시지는 닉네임으로 골라낸다
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.MEMBER_MEMBER_JOINED})
@TestPropertySource(properties =
	"spring.datasource.url=jdbc:h2:mem:bukang-member-kafka-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class MemberKafkaPublisherTest {
	private static final Duration POLL_TIMEOUT = Duration.ofSeconds(10);

	@Autowired
	private EmbeddedKafkaBroker embeddedKafka;
	@Autowired
	private MemberFacade memberFacade;
	@Autowired
	private MemberJoinUseCase memberJoinUseCase;
	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private TermsAgreementRepository termsAgreementRepository;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private JsonConverter jsonConverter;

	private Consumer<String, String> consumer;
	private int consumerCount;

	@BeforeEach
	void setUp() {
		Map<String, Object> props = KafkaTestUtils.consumerProps(
			embeddedKafka, "member-kafka-publisher-test-" + System.nanoTime(), false);
		consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
			.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.MEMBER_MEMBER_JOINED);
	}

	@AfterEach
	void tearDown() {
		consumer.close();
		termsAgreementRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("가입하면 커밋 뒤 회원 ID를 키로, 가입 이벤트 JSON을 값으로 보낸다")
	void sendJoinedEventAfterCommit() {
		MemberDto member = join("joined@crewrun.com", "가입러너", "010-2000-0001");

		ConsumerRecord<String, String> record = pollUntilNickname("가입러너", new ArrayList<>());
		MemberJoinedEvent event = jsonConverter.fromJson(record.value(), MemberJoinedEvent.class);

		assertThat(member.getId()).isNotZero();
		assertThat(record.key()).isEqualTo(String.valueOf(member.getId()));
		assertThat(event.id()).isEqualTo(member.getId());
		assertThat(event.nickname()).isEqualTo("가입러너");
		assertThat(event.createDate()).isNotNull();
		assertThat(event.modifyDate()).isNotNull();
	}

	@Test
	@DisplayName("가입 트랜잭션이 롤백되면 가입 이벤트를 보내지 않는다")
	void notSendWhenRolledBack() {
		transactionTemplate.executeWithoutResult(status -> {
			memberJoinUseCase.join(
				"rollback@crewrun.com", "password1234", "김롤백", "롤백러너", "010-2000-0002",
				LocalDate.of(1995, 4, 12), null, List.of(TermsType.SERVICE, TermsType.PRIVACY)
			);
			status.setRollbackOnly();
		});

		assertNotSentBeforeMarker("롤백러너");
		assertThat(memberRepository.existsByEmail("rollback@crewrun.com")).isFalse();
	}

	@Test
	@DisplayName("중복 이메일로 가입에 실패하면 가입 이벤트를 보내지 않는다")
	void notSendWhenJoinFails() {
		join("duplicate@crewrun.com", "먼저러너", "010-2000-0003");

		assertThatThrownBy(() -> join("duplicate@crewrun.com", "중복러너", "010-2000-0004"))
			.isInstanceOf(DuplicateEmailException.class);

		assertNotSentBeforeMarker("중복러너");
	}

	private MemberDto join(String email, String nickname, String phone) {
		return memberFacade.join(new MemberJoinRequestDto(
			email, "password1234", "김러너", nickname, phone, LocalDate.of(1995, 4, 12), null,
			List.of(TermsType.SERVICE, TermsType.PRIVACY)
		));
	}

	// 파티션이 1개라 메시지 순서가 지켜진다. 뒤에 가입한 표시 회원의 메시지가 올 때까지 읽어,
	// 그 앞에 확인할 닉네임의 메시지가 없으면 보내지 않은 것으로 본다
	private void assertNotSentBeforeMarker(String nickname) {
		join("marker" + (++consumerCount) + "@crewrun.com", "표시러너" + consumerCount, "010-2999-000" + consumerCount);

		List<ConsumerRecord<String, String>> received = new ArrayList<>();
		pollUntilNickname("표시러너" + consumerCount, received);

		assertThat(received)
			.extracting(record -> jsonConverter.fromJson(record.value(), MemberJoinedEvent.class).nickname())
			.doesNotContain(nickname);
	}

	private ConsumerRecord<String, String> pollUntilNickname(
		String nickname,
		List<ConsumerRecord<String, String>> seen
	) {
		long deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
		while (System.nanoTime() < deadline) {
			for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(consumer, Duration.ofMillis(500))) {
				seen.add(record);
				if (nickname.equals(jsonConverter.fromJson(record.value(), MemberJoinedEvent.class).nickname())) {
					return record;
				}
			}
		}
		throw new AssertionError(nickname + " 회원의 가입 이벤트를 받지 못했습니다.");
	}
}
