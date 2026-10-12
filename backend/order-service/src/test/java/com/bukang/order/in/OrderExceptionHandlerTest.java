package com.bukang.order.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.bukang.common.global.rsdata.RsData;

/**
 * 주문 서비스의 모든 API가 따르는 회원 ID 헤더 규칙을 확인한다.
 * 아직 실제 API가 없으므로, 회원 ID 헤더를 받는 테스트 전용 컨트롤러로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(OrderExceptionHandlerTest.MemberEchoController.class)
class OrderExceptionHandlerTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("회원 ID 헤더가 있으면 공통 응답 형식으로 응답한다")
	void withMemberId() throws Exception {
		mockMvc.perform(get("/test/member").header("X-Member-Id", "7"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("회원 ID를 받았습니다."))
			.andExpect(jsonPath("$.data").value(7));
	}

	@Test
	@DisplayName("회원 ID 헤더가 없으면 401과 공통 에러 형식으로 거절한다")
	void withoutMemberId() throws Exception {
		mockMvc.perform(get("/test/member"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	@DisplayName("회원 ID가 숫자가 아니면 400과 공통 에러 형식으로 거절한다")
	void invalidMemberId() throws Exception {
		mockMvc.perform(get("/test/member").header("X-Member-Id", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다."));
	}

	@RestController
	static class MemberEchoController {
		@GetMapping("/test/member")
		RsData<Integer> echo(@RequestHeader("X-Member-Id") int memberId) {
			return RsData.of(HttpStatus.OK, "회원 ID를 받았습니다.", memberId);
		}
	}
}
