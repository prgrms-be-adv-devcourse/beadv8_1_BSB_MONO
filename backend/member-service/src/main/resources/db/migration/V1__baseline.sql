-- 기준선(V1): member-service의 표를 처음 만드는 SQL입니다 (BE-024)
-- 2026-10-16 dev 기준 엔티티(Member)로 Hibernate가 만든 MySQL DDL을 옮겼습니다
-- 이미 표가 있는 dev·운영 DB에서는 실행하지 않고 baseline-on-migrate로 "1번까지 됨"으로 표시만 합니다
-- 빈 DB에서만 실제로 실행됩니다. 이 파일은 고치지 않고, 바꿀 내용은 새 V<날짜시각>__설명.sql로 더합니다

create table member_member (
	member_id integer not null auto_increment,
	created_at datetime(6) not null,
	updated_at datetime(6) not null,
	nickname varchar(16) not null,
	email varchar(255) not null,
	password_hash varchar(255) not null,
	name varchar(255) not null,
	phone varchar(255) not null,
	phone_hash varchar(64) not null,
	birth_date date not null,
	gender varchar(10),
	ci_hash varchar(64),
	verified_at datetime(6),
	primary key (member_id),
	constraint uk_member_email unique (email),
	constraint uk_member_nickname unique (nickname),
	constraint uk_member_phone_hash unique (phone_hash)
) engine=InnoDB;
