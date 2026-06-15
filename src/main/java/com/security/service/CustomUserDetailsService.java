package com.security.service;

import com.security.dto.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


// Spring Security가 로그인할 때 사용자 정보를 조회하는 서비스
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    // Config 쪽에서 @Bean으로 등록한 PasswordEncoder가 여기로
    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // Spring Security가 로그인 검증을 할 때 자동으로 호출하는 메서드
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 지금은 DB가 없으므로 임시로 "user"라는 임시
        if (!"user".equals(username)) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        // Spring Security가 이해할 수 있는 UserDetails 객체를 만들어서 반환
        return new CustomUserDetails(
                "user",
                passwordEncoder.encode("1234")
        );

        /*
         * 회원가입 및 db 되었을시 에 주석 처리된걸로 바꿀것
         * Member member = memberRepository.findByLoginId(username)
         *         .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
         *
         * return new CustomUserDetails(member);

         */
    }
}
