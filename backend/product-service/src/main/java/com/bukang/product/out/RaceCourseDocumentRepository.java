package com.bukang.product.out;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.bukang.product.domain.document.RaceCourseDocument;

public interface RaceCourseDocumentRepository extends ElasticsearchRepository<RaceCourseDocument, String> {
}
