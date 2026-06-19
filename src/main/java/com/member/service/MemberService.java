package com.member.service;

import com.member.constant.Plan;
import com.member.dto.MemberDto;
import com.member.entity.Member;
import com.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Long signUp(MemberDto memberDto) {
        if(memberRepository.findByEmail(memberDto.getEmail()).isPresent()){
            throw new IllegalArgumentException("이미 가입된 이메일 입니다");
        }
        Member member = new Member();
        member.setEmail(memberDto.getEmail());
        member.setPassword(passwordEncoder.encode(memberDto.getPassword()));
        member.setName(memberDto.getName());
        member.setPlan(Plan.FREE);

        memberRepository.save(member);
        return member.getId();
    }

    
}
