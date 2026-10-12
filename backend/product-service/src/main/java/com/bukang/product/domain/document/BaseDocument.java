package com.bukang.product.domain.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 ES 문서의 공통 필드 (Elasticsearch Auditing)
 * 문서는 DB 행을 옮겨 색인하므로, createdAt에는 DB의 등록 일시를 넣어 다시 색인해도 바뀌지 않게 한다.
 * lastModifiedAt은 저장할 때마다 Auditing이 채운다 (마지막 색인 시각).
 * 시각은 JPA 엔티티와 같은 LocalDateTime이다. 기본 DateTimeProvider가 LocalDateTime을 돌려주므로
 * JPA Auditing과 함께 써도 Provider 빈을 따로 두지 않는다.
 */
@Getter
@NoArgsConstructor
public abstract class BaseDocument implements Persistable<String> {
	@Id
	private String id;

	@CreatedDate
	@Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
	private LocalDateTime lastModifiedAt;

	protected BaseDocument(String id, LocalDateTime createdAt) {
		this.id = id;
		this.createdAt = createdAt;
	}

	// Auditing이 새 문서인지 판단하는 기준 (새 문서이면 createdAt도 채운다)
	@Override
	public boolean isNew() {
		return id == null || (createdAt == null && lastModifiedAt == null);
	}
}
