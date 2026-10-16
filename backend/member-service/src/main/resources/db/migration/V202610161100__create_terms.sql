-- 약관(member_terms)과 약관 동의 기록(member_terms_agreement) 표를 만든다 (BE-021, PRO-72)
-- Terms, TermsAgreement 엔티티와 칸 이름·종류·길이·NULL 여부를 맞춘다 (ddl-auto=validate 통과용)
-- 표 이름은 Hibernate가 쓰는 소문자(member_*)로 만든다. 기본 약관 데이터는 TermsDataInit이 앱 시작 때 넣는다

CREATE TABLE member_terms (
	id             INT          NOT NULL AUTO_INCREMENT,
	create_date    DATETIME(6)  NULL,
	modify_date    DATETIME(6)  NULL,
	type           ENUM ('MARKETING', 'PRIVACY', 'SERVICE') NOT NULL,
	version        VARCHAR(20)  NOT NULL,
	required       BIT(1)       NOT NULL,
	content_url    VARCHAR(255) NOT NULL,
	effective_date DATE         NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uk_terms_type_version UNIQUE (type, version)
);

-- 고치지 않고 쌓는 기록이라 유일 제약은 없다
CREATE TABLE member_terms_agreement (
	id          INT         NOT NULL AUTO_INCREMENT,
	create_date DATETIME(6) NULL,
	modify_date DATETIME(6) NULL,
	member_id   INT         NOT NULL,
	terms_id    INT         NOT NULL,
	agreed      BIT(1)      NOT NULL,
	decided_at  DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_terms_agreement_member FOREIGN KEY (member_id) REFERENCES member_member (member_id),
	CONSTRAINT fk_terms_agreement_terms FOREIGN KEY (terms_id) REFERENCES member_terms (id)
);
