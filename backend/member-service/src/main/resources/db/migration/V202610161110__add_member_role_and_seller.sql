-- 판매자 등록 신청·관리자 승인 (PRO-129, BE-020)
-- 1. 회원에 역할 칸(USER/ADMIN)을 더한다. 이미 있는 회원은 기본값 'USER'로 채워진다 (Member.role)
-- 2. 판매자 신청 원본 테이블을 만든다. 신청마다 새 행이고, 한 회원에 행이 여러 개일 수 있다 (Seller)
-- 테이블·칸 이름은 엔티티 매핑(Spring 기본 이름 규칙, 소문자)과 같게 두어 ddl-auto: validate를 통과한다

ALTER TABLE member_member
	ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER';

CREATE TABLE member_seller (
	seller_id INT NOT NULL AUTO_INCREMENT,
	member_id INT NOT NULL,
	seller_type VARCHAR(20) NOT NULL,
	business_name VARCHAR(100),
	business_number VARCHAR(20),
	representative_name VARCHAR(50) NOT NULL,
	status VARCHAR(20) NOT NULL,
	requested_at DATETIME(6) NOT NULL,
	reviewed_at DATETIME(6),
	reject_reason VARCHAR(500),
	reviewed_by INT,
	PRIMARY KEY (seller_id),
	CONSTRAINT fk_member_seller_member FOREIGN KEY (member_id) REFERENCES member_member (member_id)
);

-- 회원의 대기·승인 신청 확인, 가장 최근 신청 조회 (Seller의 @Index)
CREATE INDEX idx_member_seller_member ON member_seller (member_id, status);
