package com.security.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

// Spring Security가 로그인 사용자를 이해할 수 있도록 만드는 클래스
public class CustomUserDetails implements UserDetails {

    // 지금은 DB Entity가 없으므로 임시 user, 1234
    // 나중에는 Member Entity 가져올것.
    private final String username;
    private final String password;


    public CustomUserDetails(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // 지금은 권한 처리를 자세히 하지 않으므로 빈 목록을 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
    // 사용자의 비밀번호를 반환
    @Override
    public String getPassword() {
        return password;
    }
    // 사용자의 아이디를 반환
    @Override
    public String getUsername() {
        return username;
    }
    // 계정 만료 여부 (true 면 만료 X)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    // 계정 잠김 여부 (true 면 만료 X)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    // 비밀번호 만료 여부(true 면 만료 X)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    // 계정 활성화 여부(true 면 사용 가능)
    @Override
    public boolean isEnabled() {
        return true;
    }
}