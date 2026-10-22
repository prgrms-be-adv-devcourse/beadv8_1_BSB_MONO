package com.bukang.payout.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bukang.payout.domain.exception.PayoutMemberNotFoundException;
import com.bukang.payout.domain.exception.PayoutMemberUnavailableException;

/**
 * 회원 서비스에 관리자 여부를 묻는 어댑터 (PRO-265)
 * 핵심은 '모르는 경우'를 관리자로 보지 않는 것이다: 타임아웃 · 5xx · 빈 응답은 모두 503으로 막는다
 */
class MemberAdminAdapterTest {
	private static final String BASE_URL = "http://member-service";
	private static final String URL = BASE_URL + "/internal/v1/member/members/8/admin";

	private MockRestServiceServer server;
	private MemberAdminAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new MemberAdminAdapter(builder.build());
	}

	@Test
	@DisplayName("회원 ID를 경로에 담아 GET으로 묻고, admin=true면 관리자다")
	void admin() {
		server.expect(requestTo(URL))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(body(true), MediaType.APPLICATION_JSON));

		assertThat(adapter.isAdmin(8)).isTrue();
		server.verify();
	}

	@Test
	@DisplayName("일반 회원이면 회원 서비스가 200 admin=false로 답하고, 그대로 false다")
	void notAdmin() {
		server.expect(requestTo(URL)).andRespond(withSuccess(body(false), MediaType.APPLICATION_JSON));

		assertThat(adapter.isAdmin(8)).isFalse();
	}

	@Test
	@DisplayName("없거나 탈퇴한 회원이라 404면 PayoutMemberNotFoundException(404)이다")
	void notFound() {
		server.expect(requestTo(URL))
			.andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("""
				{ "status": 404, "code": "MEMBER_NOT_FOUND", "message": "존재하지 않는 회원입니다.", "data": null }
				"""));

		assertThatThrownBy(() -> adapter.isAdmin(8)).isInstanceOf(PayoutMemberNotFoundException.class);
	}

	@Test
	@DisplayName("응답이 늦어 타임아웃이 나면 관리자로 보지 않고 503으로 막는다")
	void timeoutIsUnavailable() {
		server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

		assertThatThrownBy(() -> adapter.isAdmin(8))
			.isInstanceOf(PayoutMemberUnavailableException.class)
			.hasRootCauseInstanceOf(SocketTimeoutException.class);
	}

	@Test
	@DisplayName("회원 서비스가 5xx면 503으로 막는다")
	void serverErrorIsUnavailable() {
		server.expect(requestTo(URL)).andRespond(withServerError());

		assertThatThrownBy(() -> adapter.isAdmin(8)).isInstanceOf(PayoutMemberUnavailableException.class);
	}

	@Test
	@DisplayName("404가 아닌 4xx(예: 400)도 관리자인지 모르므로 503으로 막는다")
	void otherClientErrorIsUnavailable() {
		server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

		assertThatThrownBy(() -> adapter.isAdmin(8)).isInstanceOf(PayoutMemberUnavailableException.class);
	}

	@Test
	@DisplayName("응답의 data나 admin이 비어 있으면 503으로 막는다")
	void emptyBodyIsUnavailable() {
		server.expect(requestTo(URL)).andRespond(withSuccess("""
			{ "status": 200, "message": "관리자 여부를 조회했습니다.", "data": null }
			""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> adapter.isAdmin(8)).isInstanceOf(PayoutMemberUnavailableException.class);
	}

	private static String body(boolean admin) {
		return """
			{ "status": 200, "message": "관리자 여부를 조회했습니다.", "data": { "admin": %s } }
			""".formatted(admin);
	}
}
