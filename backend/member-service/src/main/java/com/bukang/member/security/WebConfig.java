package com.bukang.member.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebConfig {

	// 로그인 상태는 세션이 아니라 토큰으로 유지한다
	// - Access Token은 게이트웨이(JwtAuthenticationFilter)가 검사하고, 통과하면 회원 ID를 X-Member-Id 헤더로 넘긴다
	// - 로그인이 필요한 API는 컨트롤러에서 @RequestHeader("X-Member-Id")로 회원 ID를 받는다. 헤더가 없으면 로그인하지 않은 요청이다
	// - 서비스 포트는 외부에 열지 않으므로(compose.yml) 이 헤더는 게이트웨이를 거친 요청에만 들어 있다
	// 그래서 여기서는 주소별로 막지 않는다. Spring Security는 로그인 때 비밀번호 확인(AuthenticationManager)에만 쓴다
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			// 세션을 만들지도, 세션에서 로그인 상태를 읽지도 않는다
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(
			AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

}
