-- 회원 탈퇴 (PRO-130, BE-022)
-- 1. 상태(status)와 탈퇴 시각(withdrawn_at) 칸을 더한다. 기존 회원은 기본값으로 ACTIVE가 된다
-- 2. 탈퇴하면 비우는 개인정보 칸을 NULL 허용으로 바꾼다. 타입·길이는 엔티티 매핑(SourceMember, Member) 그대로 둔다
--    (gender는 가입 때부터 선택이라 이미 NULL 허용이므로 바꾸지 않는다)
-- 3. 탈퇴(WITHDRAWN)가 아닌 회원은 개인정보 칸이 모두 있어야 한다는 CHECK를 건다.
--    이름은 Member의 @Table(check = @CheckConstraint(name = "ck_member_personal_info"))와 같다
-- 한 ALTER에 여러 변경을 쉼표로 잇는 문법은 H2(MySQL 모드)가 읽지 못해, MySQL·H2 모두 되는 한 줄 한 변경으로 쓴다
ALTER TABLE member_member ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE member_member ADD COLUMN withdrawn_at DATETIME(6) NULL;

ALTER TABLE member_member MODIFY COLUMN email VARCHAR(255) NULL;
ALTER TABLE member_member MODIFY COLUMN name VARCHAR(255) NULL;
ALTER TABLE member_member MODIFY COLUMN nickname VARCHAR(16) NULL;
ALTER TABLE member_member MODIFY COLUMN phone VARCHAR(255) NULL;
ALTER TABLE member_member MODIFY COLUMN phone_hash VARCHAR(64) NULL;
ALTER TABLE member_member MODIFY COLUMN birth_date DATE NULL;

ALTER TABLE member_member ADD CONSTRAINT ck_member_personal_info CHECK (
	status = 'WITHDRAWN'
	OR (email IS NOT NULL AND name IS NOT NULL AND nickname IS NOT NULL
		AND phone IS NOT NULL AND phone_hash IS NOT NULL AND birth_date IS NOT NULL)
);
