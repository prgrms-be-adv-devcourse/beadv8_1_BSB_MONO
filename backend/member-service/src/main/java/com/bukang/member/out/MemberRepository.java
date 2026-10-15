package com.bukang.member.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {
	Optional<Member> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);

	boolean existsByPhoneHash(String phoneHash);

	// 정보 수정 때 내 행을 빼고 중복을 찾는다
	boolean existsByNicknameAndIdNot(String nickname, int id);

	boolean existsByPhoneHashAndIdNot(String phoneHash, int id);
}
