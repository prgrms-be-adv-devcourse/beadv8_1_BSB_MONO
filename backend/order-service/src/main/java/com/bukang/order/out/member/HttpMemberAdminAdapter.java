package com.bukang.order.out.member;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.bukang.common.global.rsdata.RsData;
import com.bukang.order.domain.exception.ExternalServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 서비스의 관리자 여부 내부 API를 부른다 (GET /internal/v1/member/members/{memberId}/admin, BUK-26)
 * - 200: admin 값 그대로 (일반 회원이어도 200 false)
 * - 404: 없거나 탈퇴한 회원 → 관리자가 아니다 (403은 주문이 정한다)
 * - 그 밖(시간 초과 · 연결 실패 · 5xx · 그 밖의 4xx · 빈 응답): 관리자인지 모르므로 503으로 다시 시도하게 한다
 * 등록은 MemberClientConfig가 한다 (order.clients.member.mode=http)
 */
@Slf4j
@RequiredArgsConstructor
public class HttpMemberAdminAdapter implements MemberAdminPort {
	static final String PATH = "/internal/v1/member/members/{memberId}/admin";
	static final String UNAVAILABLE_MESSAGE = "회원 서비스에 연결하지 못했습니다. 잠시 뒤 다시 시도해주세요.";
	private static final ParameterizedTypeReference<RsData<AdminResponse>> RESPONSE_TYPE =
		new ParameterizedTypeReference<>() {
		};

	private final RestClient restClient;

	@Override
	public boolean isAdmin(int memberId) {
		RsData<AdminResponse> response;
		try {
			response = restClient.get()
				.uri(PATH, memberId)
				.retrieve()
				.body(RESPONSE_TYPE);
		} catch (HttpClientErrorException.NotFound exception) {
			return false;
		} catch (HttpClientErrorException exception) {
			// 4xx는 다시 시도해도 풀리지 않는다. 경로 계약이 어긋났을 수 있어 장애와 구분해 남긴다
			log.error("회원 서비스가 관리자 여부 요청을 거절했습니다. 계약(경로)을 확인해야 합니다. status={}, memberId={}",
				exception.getStatusCode(), memberId, exception);
			throw new ExternalServiceUnavailableException(UNAVAILABLE_MESSAGE);
		} catch (RestClientException exception) {
			log.error("회원 서비스에 관리자 여부를 묻지 못했습니다. memberId={}", memberId, exception);
			throw new ExternalServiceUnavailableException(UNAVAILABLE_MESSAGE);
		}
		if (response == null || response.data() == null || response.data().admin() == null) {
			log.error("회원 서비스의 관리자 여부 응답에 값이 없습니다. memberId={}, response={}", memberId, response);
			throw new ExternalServiceUnavailableException(UNAVAILABLE_MESSAGE);
		}
		return response.data().admin();
	}

	// 회원 서비스 응답의 data (member MemberAdminDto). 빈 응답을 구분하려고 Boolean으로 받는다
	@JsonIgnoreProperties(ignoreUnknown = true)
	record AdminResponse(Boolean admin) {
	}
}
