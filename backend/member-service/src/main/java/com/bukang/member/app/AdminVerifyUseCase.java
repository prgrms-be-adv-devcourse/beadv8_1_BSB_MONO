package com.bukang.member.app;

import org.springframework.stereotype.Service;

import com.bukang.member.domain.Member;
import com.bukang.member.domain.exception.MemberNotAdminException;
import com.bukang.member.domain.exception.MemberNotFoundException;
import com.bukang.member.out.MemberRepository;

import lombok.RequiredArgsConstructor;

// 관리자 API를 부른 회원이 관리자인지 DB의 역할 칸으로 확인한다 (BE-020 결정 1)
// 토큰에는 역할이 없어서, 게이트웨이가 넘겨준 X-Member-Id로 회원을 읽어 본다
@Service
@RequiredArgsConstructor
public class AdminVerifyUseCase {
	private final MemberRepository memberRepository;

	public Member verify(int memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException("존재하지 않는 회원입니다."));
		if (!member.isAdmin()) {
			throw new MemberNotAdminException("관리자만 이용할 수 있습니다.");
		}
		return member;
	}
}
