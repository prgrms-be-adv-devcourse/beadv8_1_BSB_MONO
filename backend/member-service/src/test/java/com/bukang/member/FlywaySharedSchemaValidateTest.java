package com.bukang.member;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// 모든 서비스가 같은 스키마를 쓰므로, 다른 서비스 표만 먼저 있는 DB에서 member가 처음 뜨는 경우를 확인한다 (BE-024)
// init-sqls로 Flyway보다 먼저 다른 서비스 표를 만들어 "기록 표는 없고 스키마는 비어 있지 않은" 상태를 만든다
@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:flyway-shared;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
	"spring.flyway.enabled=true",
	"spring.flyway.init-sqls=create table if not exists product_race (race_id integer not null primary key)",
	"spring.flyway.table=flyway_schema_history_member",
	"spring.flyway.baseline-on-migrate=true",
	"spring.flyway.baseline-version=0",
	"spring.flyway.out-of-order=true",
	"spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
class FlywaySharedSchemaValidateTest {

	@Autowired
	private Flyway flyway;

	@Test
	@DisplayName("다른 서비스 표만 있는 DB에서도 member 표가 만들어져 엔티티와 맞는다 (Hibernate validate 통과)")
	void createsMemberTablesInSharedSchema() {
		// validate에 실패하면 컨텍스트가 뜨지 않아 이 테스트까지 오지 못한다
		assertThat(flyway.info().pending()).isEmpty();
		assertThat(flyway.info().current().getVersion().getVersion()).isNotEqualTo("0");
	}
}
