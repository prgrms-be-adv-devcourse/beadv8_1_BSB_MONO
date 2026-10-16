package com.bukang.member.config.swagger;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bukang.member.in.MemberIdHeader;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger 문서에 로그인 방식(Bearer JWT)과 X-Member-Id 헤더 설명을 붙인다
 * 클라이언트는 Access Token만 보내고, 게이트웨이가 토큰을 검사해 X-Member-Id를 넣는다. Swagger는 dev에서 서비스를 직접
 * 띄워 보므로 게이트웨이가 없어 헤더를 직접 넣어야 한다. 그래서 헤더는 숨기지 않고 "게이트웨이가 넣는 헤더"라고 설명한다
 * X-Member-Id를 받는 API에만 자물쇠(Bearer)를 표시해, 컨트롤러마다 따로 적지 않아도 로그인 필요 여부가 문서에 맞게 나온다
 */
@Configuration
public class MemberSwaggerConfig {
	static final String BEARER_AUTH = "bearerAuth";
	static final String MEMBER_ID_HEADER_DESCRIPTION =
		"게이트웨이가 Access Token에서 꺼내 넣는 회원 ID입니다. 게이트웨이를 거치면 클라이언트는 보내지 않고 "
			+ "Authorization: Bearer 토큰만 보냅니다. 서비스를 직접 띄운 이 Swagger에서만 직접 입력합니다.";

	@Bean
	public OpenApiCustomizer memberAuthCustomizer() {
		return openApi -> {
			if (openApi.getComponents() == null) {
				openApi.setComponents(new Components());
			}
			openApi.getComponents().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.description("로그인 응답의 Access Token. 게이트웨이가 검사한 뒤 X-Member-Id를 넣어 서비스로 넘깁니다."));

			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths().values().forEach(pathItem ->
				pathItem.readOperations().forEach(MemberSwaggerConfig::markMemberIdOperation));
		};
	}

	// X-Member-Id 헤더를 받는 API면 헤더에 설명을 붙이고 Bearer 로그인이 필요하다고 표시한다
	private static void markMemberIdOperation(Operation operation) {
		if (operation.getParameters() == null) {
			return;
		}
		boolean needsLogin = false;
		for (Parameter parameter : operation.getParameters()) {
			if ("header".equals(parameter.getIn()) && MemberIdHeader.NAME.equals(parameter.getName())) {
				parameter.setDescription(MEMBER_ID_HEADER_DESCRIPTION);
				needsLogin = true;
			}
		}
		if (needsLogin) {
			operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
		}
	}
}
