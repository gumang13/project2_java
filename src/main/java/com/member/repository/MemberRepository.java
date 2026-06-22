package com.member.repository;

import com.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {
        // email로 회원 찾기
    Optional<Member> findByEmail(String email);

    // 회원가입때 이메일 중복 확인
    boolean existsByEmail(String email);


}
