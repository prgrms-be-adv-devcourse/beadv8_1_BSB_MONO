package com.bukang.member.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.bukang.common.shared.seller.domain.SellerStatus;
import com.bukang.common.shared.seller.domain.SellerType;
import com.bukang.common.shared.seller.event.SellerChangedEvent;
import com.bukang.member.app.MemberFacade;
import com.bukang.member.app.MemberJoinUseCase;
import com.bukang.member.app.SellerApplyUseCase;
import com.bukang.member.app.SellerFacade;
import com.bukang.member.domain.exception.DuplicateEmailException;
import com.bukang.member.dto.MemberDto;
import com.bukang.member.dto.MemberJoinRequestDto;
import com.bukang.member.dto.SellerApplyRequestDto;
import com.bukang.member.dto.SellerDto;

// 실제 임베디드 Kafka 브로커로 가입 이벤트가 커밋 뒤에만 queuing.member.joined에,
// 판매자 변경 이벤트가 커밋 뒤에만 queuing.seller.changed에 나가는지 확인한다.
// - 다른 테스트의 공유 H2(bukang-test)와 분리된 전용 DB를 쓴다 (create-drop이 다른 테스트 데이터를 건드리지 않게)
// - 테스트마다 새 컨슈머 그룹으로 토픽을 처음부터 읽으므로, 메시지는 닉네임으로 골라낸다
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.MEMBER_MEMBER_JOINED, KafkaTopics.SELLER_CHANGED})
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
	private SellerFacade sellerFacade;
	@Autowired
	private SellerApplyUseCase sellerApplyUseCase;
	@Autowired
	private SellerRepository sellerRepository;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private JsonConverter jsonConverter;

	private Consumer<String, String> consumer;
	private Consumer<String, String> sellerConsumer;
	private int consumerCount;

	@BeforeEach
	void setUp() {
		Map<String, Object> props = KafkaTestUtils.consumerProps(
			embeddedKafka, "member-kafka-publisher-test-" + System.nanoTime(), false);
		consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
			.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.MEMBER_MEMBER_JOINED);

		Map<String, Object> sellerProps = KafkaTestUtils.consumerProps(
			embeddedKafka, "member-kafka-publisher-seller-test-" + System.nanoTime(), false);
		sellerConsumer = new DefaultKafkaConsumerFactory<>(sellerProps, new StringDeserializer(),
			new StringDeserializer()).createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(sellerConsumer, KafkaTopics.SELLER_CHANGED);
	}

	@AfterEach
	void tearDown() {
		consumer.close();
		sellerConsumer.close();
		// 판매자가 회원을 참조하므로 먼저 지운다
		sellerRepository.deleteAll();
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
				LocalDate.of(1995, 4, 12), null
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

	@Test
	@DisplayName("판매자 신청·승인은 커밋 뒤 판매자 ID를 키로, 판매자 변경 이벤트 JSON을 값으로 보낸다")
	void sendSellerChangedEventAfterCommit() {
		MemberDto member = join("seller@crewrun.com", "판매러너", "010-2000-0011");
		MemberDto admin = join("admin@crewrun.com", "관리러너", "010-2000-0012");
		memberFacade.grantAdmin("admin@crewrun.com");

		SellerDto seller = applySeller(member.getId(), "신청대표");
		ConsumerRecord<String, String> applied = pollUntilRepresentative("신청대표", SellerStatus.PENDING);
		sellerFacade.approve(admin.getId(), seller.getId());
		ConsumerRecord<String, String> approved = pollUntilRepresentative("신청대표", SellerStatus.APPROVED);

		SellerChangedEvent appliedEvent = jsonConverter.fromJson(applied.value(), SellerChangedEvent.class);
		SellerChangedEvent approvedEvent = jsonConverter.fromJson(approved.value(), SellerChangedEvent.class);
		assertThat(applied.key()).isEqualTo(String.valueOf(seller.getId()));
		assertThat(approved.key()).isEqualTo(String.valueOf(seller.getId()));
		assertThat(appliedEvent.id()).isEqualTo(seller.getId());
		assertThat(appliedEvent.memberId()).isEqualTo(member.getId());
		assertThat(appliedEvent.sellerType()).isEqualTo(SellerType.BUSINESS);
		assertThat(appliedEvent.businessNumber()).isEqualTo("123-45-67890");
		assertThat(appliedEvent.requestedAt()).isNotNull();
		assertThat(appliedEvent.reviewedAt()).isNull();
		assertThat(approvedEvent.reviewedAt()).isNotNull();
	}

	@Test
	@DisplayName("판매자 신청 트랜잭션이 롤백되면 판매자 변경 이벤트를 보내지 않는다")
	void notSendSellerChangedWhenRolledBack() {
		MemberDto member = join("seller-rollback@crewrun.com", "롤백판매", "010-2000-0021");
		transactionTemplate.executeWithoutResult(status -> {
			sellerApplyUseCase.apply(member.getId(), SellerType.INDIVIDUAL, null, null, "롤백대표",
				LocalDateTime.now());
			status.setRollbackOnly();
		});

		// 같은 회원으로 다시 신청할 수 있어야 롤백된 것이다. 이 신청이 뒤따르는 표시 메시지가 된다
		applySeller(member.getId(), "표시대표");
		List<ConsumerRecord<String, String>> received = new ArrayList<>();
		pollUntilRepresentative("표시대표", SellerStatus.PENDING, received);

		assertThat(received)
			.extracting(record -> jsonConverter.fromJson(record.value(), SellerChangedEvent.class).representativeName())
			.doesNotContain("롤백대표");
	}

	private SellerDto applySeller(int memberId, String representativeName) {
		return sellerFacade.apply(memberId,
			new SellerApplyRequestDto(SellerType.BUSINESS, "크루런 스포츠", "1234567890", representativeName));
	}

	private ConsumerRecord<String, String> pollUntilRepresentative(String representativeName, SellerStatus status) {
		return pollUntilRepresentative(representativeName, status, new ArrayList<>());
	}

	// 대표자 이름과 상태로 메시지를 골라낸다 (테스트마다 토픽을 처음부터 읽으므로)
	private ConsumerRecord<String, String> pollUntilRepresentative(
		String representativeName,
		SellerStatus status,
		List<ConsumerRecord<String, String>> seen
	) {
		long deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
		while (System.nanoTime() < deadline) {
			for (ConsumerRecord<String, String> record
				: KafkaTestUtils.getRecords(sellerConsumer, Duration.ofMillis(500))) {
				seen.add(record);
				SellerChangedEvent event = jsonConverter.fromJson(record.value(), SellerChangedEvent.class);
				if (representativeName.equals(event.representativeName()) && event.status() == status) {
					return record;
				}
			}
		}
		throw new AssertionError(representativeName + " 판매자의 " + status + " 이벤트를 받지 못했습니다.");
	}

	private MemberDto join(String email, String nickname, String phone) {
		return memberFacade.join(new MemberJoinRequestDto(
			email, "password1234", "김러너", nickname, phone, LocalDate.of(1995, 4, 12), null
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
