package com.security.service;

import com.member.entity.Member;
import com.member.repository.MemberRepository;
import com.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


// Spring Security가 로그인할 때 사용자 정보를 조회하는 서비스
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;



    // Spring Security가 로그인 검증을 할 때 자동으로 호출하는 메서드
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // DB에서 찾은 진짜 회원 정보로 UserDetails 생성
        return new CustomUserDetails(member.getEmail(), member.getPassword());


    }
}
