package com.bukang.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.bukang.jwt.JwtProperties;
import com.bukang.jwt.JwtProvider;
import com.sun.net.httpserver.HttpServer;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// 모든 서비스 주소를 가짜 서버 하나로 돌려 두고, 게이트웨이가 토큰을 검사해 회원 ID를 넘기는지 확인한다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayAuthenticationTest {

	private static final List<String> SERVICES = List.of("member", "cash", "payout", "file", "order", "product");
	private static final String MEMBER_ID_HEADER = "X-Member-Id";
	// 가짜 서버가 받은 X-Member-Id 헤더 값 (없으면 "없음")
	private static final List<String> RECEIVED_MEMBER_IDS = new CopyOnWriteArrayList<>();
	private static HttpServer stub;

	private final HttpClient client = HttpClient.newHttpClient();

	@Value("${local.server.port}")
	private int gatewayPort;

	@Value("${jwt.secret}")
	private String secret;

	@Autowired
	private JwtProvider jwtProvider;

	@DynamicPropertySource
	static void serviceUris(DynamicPropertyRegistry registry) throws IOException {
		stub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		stub.createContext("/", exchange -> {
			String memberId = exchange.getRequestHeaders().getFirst(MEMBER_ID_HEADER);
			RECEIVED_MEMBER_IDS.add(memberId == null ? "없음" : memberId);
			byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		stub.start();
		for (String service : SERVICES) {
			registry.add("gateway.services." + service, () -> "http://localhost:" + stub.getAddress().getPort());
		}
	}

	@AfterAll
	static void stopStub() {
		stub.stop(0);
	}

	@BeforeEach
	void clearReceived() {
		RECEIVED_MEMBER_IDS.clear();
	}

	@Test
	@DisplayName("유효한 토큰이면 토큰 속 회원 ID를 X-Member-Id 헤더로 서비스에 넘긴다")
	void forwardsMemberIdForValidToken() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/order/orders")
			.header("Authorization", "Bearer " + jwtProvider.issueAccessToken(42L)));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED_MEMBER_IDS).containsExactly("42");
	}

	@Test
	@DisplayName("클라이언트가 X-Member-Id를 직접 넣어도 토큰 속 회원 ID로 바꿔 넘긴다")
	void replacesSpoofedMemberIdWithTokenMemberId() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/order/orders")
			.header("Authorization", "Bearer " + jwtProvider.issueAccessToken(42L))
			.header(MEMBER_ID_HEADER, "999"));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED_MEMBER_IDS).containsExactly("42");
	}

	@Test
	@DisplayName("토큰이 없으면 서비스로 넘기지 않고 401을 RsData 형식으로 응답한다")
	void rejectsRequestWithoutToken() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/order/orders"));

		assertUnauthorized(response, "로그인이 필요합니다.");
	}

	@Test
	@DisplayName("토큰 없이 X-Member-Id만 넣어 보내도 401로 응답한다")
	void rejectsSpoofedMemberIdWithoutToken() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/order/orders").header(MEMBER_ID_HEADER, "999"));

		assertUnauthorized(response, "로그인이 필요합니다.");
	}

	@ParameterizedTest(name = "Authorization: {0}")
	@DisplayName("Bearer 형식이 아니거나 서명이 다른 토큰이면 401로 응답한다")
	@ValueSource(strings = {"Bearer not-a-jwt", "Basic abc", "Bearer "})
	void rejectsMalformedToken(String authorization) throws Exception {
		HttpResponse<String> response = send(request("/api/v1/order/orders").header("Authorization", authorization));

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(RECEIVED_MEMBER_IDS).isEmpty();
	}

	@Test
	@DisplayName("다른 키로 서명한 토큰이면 401로 응답한다")
	void rejectsTokenSignedWithOtherKey() throws Exception {
		JwtProvider otherProvider = new JwtProvider(
			new JwtProperties("other-secret-key-0000-1111-2222-3333", Duration.ofMinutes(30), Duration.ofDays(14)),
			Clock.systemUTC());

		HttpResponse<String> response = send(request("/api/v1/order/orders")
			.header("Authorization", "Bearer " + otherProvider.issueAccessToken(42L)));

		assertUnauthorized(response, "유효하지 않은 토큰입니다.");
	}

	@Test
	@DisplayName("만료된 토큰이면 재발급할 수 있도록 만료 메시지로 401을 응답한다")
	void rejectsExpiredToken() throws Exception {
		Clock pastClock = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
		JwtProvider pastProvider = new JwtProvider(
			new JwtProperties(secret, Duration.ofMinutes(30), Duration.ofDays(14)), pastClock);

		HttpResponse<String> response = send(request("/api/v1/order/orders")
			.header("Authorization", "Bearer " + pastProvider.issueAccessToken(42L)));

		assertUnauthorized(response, "만료된 토큰입니다.");
	}

	@Test
	@DisplayName("Refresh Token을 Access Token 자리에 쓰면 401로 응답한다")
	void rejectsRefreshToken() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/order/orders")
			.header("Authorization", "Bearer " + refreshToken(42L)));

		assertUnauthorized(response, "유효하지 않은 토큰입니다.");
	}

	@ParameterizedTest(name = "{0}")
	@DisplayName("회원가입·로그인·토큰 재발급·로그아웃은 토큰 없이 통과하고, 클라이언트가 보낸 X-Member-Id는 지운다")
	@ValueSource(strings = {"/api/v1/auth/join", "/api/v1/auth/login", "/api/v1/auth/reissue", "/api/v1/auth/logout"})
	void passesPublicPathWithoutToken(String path) throws Exception {
		HttpResponse<String> response = send(request(path)
			.header(MEMBER_ID_HEADER, "999")
			.POST(HttpRequest.BodyPublishers.noBody()));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED_MEMBER_IDS).containsExactly("없음");
	}

	@Test
	@DisplayName("공개 주소와 앞부분만 같은 주소는 토큰이 필요하다")
	void requiresTokenForPathOnlyStartingWithPublicPath() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/auth/login-history")
			.POST(HttpRequest.BodyPublishers.noBody()));

		assertUnauthorized(response, "로그인이 필요합니다.");
	}

	@Test
	@DisplayName("상품 서비스 GET 요청은 로그인 없이 통과하고, 클라이언트가 보낸 X-Member-Id는 지운다")
	void passesProductReadWithoutToken() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/product/races/1").header(MEMBER_ID_HEADER, "999"));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED_MEMBER_IDS).containsExactly("없음");
	}

	@Test
	@DisplayName("상품 서비스 GET 요청에 유효한 토큰이 있으면 회원 ID를 넘긴다 (이미 신청함 표시 등에 쓴다)")
	void forwardsMemberIdForProductReadWithToken() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/product/races/1")
			.header("Authorization", "Bearer " + jwtProvider.issueAccessToken(42L)));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED_MEMBER_IDS).containsExactly("42");
	}

	@Test
	@DisplayName("상품 서비스 GET 요청에 만료된 토큰이 있으면 로그인하지 않은 요청으로 넘긴다")
	void passesProductReadWithExpiredTokenAsAnonymous() throws Exception {
		Clock pastClock = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
		JwtProvider pastProvider = new JwtProvider(
			new JwtProperties(secret, Duration.ofMinutes(30), Duration.ofDays(14)), pastClock);

		HttpResponse<String> response = send(request("/api/v1/product/races")
			.header("Authorization", "Bearer " + pastProvider.issueAccessToken(42L)));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED_MEMBER_IDS).containsExactly("없음");
	}

	@Test
	@DisplayName("상품 서비스라도 GET이 아닌 요청(대회 등록 등)은 토큰이 필요하다")
	void requiresTokenForProductWrite() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/product/races")
			.POST(HttpRequest.BodyPublishers.noBody()));

		assertUnauthorized(response, "로그인이 필요합니다.");
	}

	@ParameterizedTest(name = "{0}")
	@DisplayName("파일 조회 GET(대회 사진 등)은 로그인 없이 통과한다")
	@ValueSource(strings = {"/api/v1/file/files/1", "/api/v1/file/files?refType=Race&refId=1"})
	void passesFileReadWithoutToken(String path) throws Exception {
		HttpResponse<String> response = send(request(path));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED_MEMBER_IDS).containsExactly("없음");
	}

	@Test
	@DisplayName("파일 업로드 등 GET이 아닌 파일 요청은 토큰이 필요하다")
	void requiresTokenForFileWrite() throws Exception {
		HttpResponse<String> response = send(request("/api/v1/file/files")
			.POST(HttpRequest.BodyPublishers.noBody()));

		assertUnauthorized(response, "로그인이 필요합니다.");
	}

	// 게이트웨이는 Refresh Token을 발급하지 않으므로, 발급 규칙(jti·familyId 등)에 기대지 않고 type만 REFRESH인 토큰을 직접 서명한다
	private String refreshToken(long memberId) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim("type", "REFRESH")
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(Duration.ofDays(1))))
			.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
			.compact();
	}

	private void assertUnauthorized(HttpResponse<String> response, String message) {
		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
			contentType -> assertThat(contentType).startsWith("application/json"));
		assertThat(response.body()).isEqualTo("{\"status\":401,\"message\":\"" + message + "\",\"data\":null}");
		assertThat(RECEIVED_MEMBER_IDS).isEmpty();
	}

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + path));
	}

	private HttpResponse<String> send(HttpRequest.Builder builder) throws IOException, InterruptedException {
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

}
