package com.bukang.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpServer;

// 서비스마다 가짜 서버를 하나씩 띄우고, 게이트웨이가 요청을 어느 서버로 어떤 주소로 넘기는지 확인한다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingTest {

	private static final List<String> SERVICES = List.of("member", "cash", "payout", "file", "order", "product");
	private static final List<HttpServer> STUBS = new CopyOnWriteArrayList<>();
	// 가짜 서버가 받은 요청을 "서비스이름 경로" 형식으로 남긴다
	private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();
	// 가짜 서버가 받은 X-Member-Id 헤더 값 (없으면 "없음")
	private static final List<String> RECEIVED_MEMBER_IDS = new CopyOnWriteArrayList<>();

	private final HttpClient client = HttpClient.newHttpClient();

	@Value("${local.server.port}")
	private int gatewayPort;

	@DynamicPropertySource
	static void serviceUris(DynamicPropertyRegistry registry) throws IOException {
		for (String service : SERVICES) {
			HttpServer stub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			stub.createContext("/", exchange -> {
				RECEIVED.add(service + " " + exchange.getRequestURI());
				String memberId = exchange.getRequestHeaders().getFirst("X-Member-Id");
				RECEIVED_MEMBER_IDS.add(memberId == null ? "없음" : memberId);
				byte[] body = service.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, body.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(body);
				}
			});
			stub.start();
			STUBS.add(stub);
			registry.add("gateway.services." + service, () -> "http://localhost:" + stub.getAddress().getPort());
		}
	}

	@AfterAll
	static void stopStubs() {
		STUBS.forEach(stub -> stub.stop(0));
	}

	@BeforeEach
	void clearReceived() {
		RECEIVED.clear();
		RECEIVED_MEMBER_IDS.clear();
	}

	@ParameterizedTest(name = "{0} → {1}")
	@DisplayName("경로에 맞는 서비스로 주소를 자르지 않고 그대로 넘긴다")
	@CsvSource({
		"/api/v1/member/members/1, member",
		"/api/v1/auth/login, member",
		"/api/v1/cash/wallets, cash",
		"/api/v1/payout/payouts, payout",
		"/api/v1/file/files/1, file",
		"/api/v1/order/orders?page=0, order",
		"/api/v1/product/races/1, product"
	})
	void routesToServiceWithoutStrippingPath(String path, String service) throws Exception {
		HttpResponse<String> response = get(path);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED).containsExactly(service + " " + path);
	}

	@Test
	@DisplayName("내부 전용 주소(/internal/**)는 서비스로 넘기지 않고 404로 응답한다")
	void doesNotRouteInternalPath() throws Exception {
		HttpResponse<String> response = get("/internal/v1/file/refs/RACE/1/files");

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(RECEIVED).isEmpty();
	}

	@Test
	@DisplayName("라우팅 규칙에 없는 주소는 404로 응답한다")
	void respondsNotFoundForUnknownPath() throws Exception {
		HttpResponse<String> response = get("/api/v1/unknown/1");

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(RECEIVED).isEmpty();
	}

	@Test
	@DisplayName("클라이언트가 보낸 X-Member-Id 헤더는 지우고 서비스로 넘긴다")
	void removesMemberIdHeaderFromClient() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/api/v1/order/orders"))
			.header("X-Member-Id", "999")
			.GET()
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(RECEIVED_MEMBER_IDS).containsExactly("없음");
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + path)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
