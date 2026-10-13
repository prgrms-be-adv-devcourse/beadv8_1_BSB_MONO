package com.bukang.member.app;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bukang.member.domain.Member;
import com.bukang.member.domain.exception.MemberNotFoundException;
import com.bukang.member.dto.AuthTokens;
import com.bukang.member.dto.MemberDto;
import com.bukang.member.dto.MemberJoinRequestDto;
import com.bukang.member.dto.MemberLoginRequestDto;
import com.bukang.member.dto.MemberLoginResult;
import com.bukang.member.out.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberFacade {
	private final MemberJoinUseCase memberJoinUseCase;
	private final MemberRepository memberRepository;
	private final AuthenticationManager authenticationManager;
	private final TokenIssueUseCase tokenIssueUseCase;
	private final TokenReissueUseCase tokenReissueUseCase;
	private final LogoutUseCase logoutUseCase;

	@Transactional
	public MemberDto join(MemberJoinRequestDto memberJoinDto) {
		return memberJoinUseCase.join(
			memberJoinDto.getEmail(),
			memberJoinDto.getPassword(),
			memberJoinDto.getName(),
			memberJoinDto.getNickname(),
			memberJoinDto.getPhone(),
			memberJoinDto.getBirthDate(),
			memberJoinDto.getGender()
		).toDto();
	}

	public boolean existsByEmail(String email) {
		return memberRepository.existsByEmail(email);
	}

	public MemberLoginResult login(MemberLoginRequestDto memberLoginRequestDto) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				memberLoginRequestDto.getEmail(),
				memberLoginRequestDto.getPassword()
			)
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Member member = memberRepository.findByEmail(memberLoginRequestDto.getEmail())
			.orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));

		return new MemberLoginResult(member.toDto(), tokenIssueUseCase.issue(member.getId()));
	}

	// Refresh Token도 회전하므로 Access Token과 새 Refresh Token을 함께 돌려준다
	public AuthTokens reissue(String refreshToken) {
		return tokenReissueUseCase.reissue(refreshToken);
	}

	public void logout(String refreshToken) {
		logoutUseCase.logout(refreshToken);
	}
}
