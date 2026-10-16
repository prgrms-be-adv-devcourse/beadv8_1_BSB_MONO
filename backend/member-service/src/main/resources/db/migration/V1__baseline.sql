-- 기준선(V1): member-service의 표를 처음 만드는 SQL입니다 (BE-024)
-- 2026-10-16 dev 기준 엔티티(Member)로 Hibernate가 만든 MySQL DDL을 옮겼습니다
-- 모든 서비스가 같은 스키마를 써서, 다른 서비스 표만 있고 member_member는 없는 DB가 있을 수 있습니다.
-- 그래서 기준선은 0번으로 두고 이 V1은 항상 실행하되, 표가 이미 있으면 아무것도 하지 않습니다(if not exists)
-- 이 파일은 고치지 않고, 바꿀 내용은 새 V<날짜시각>__설명.sql로 더합니다

create table if not exists member_member (
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
