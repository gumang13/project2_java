package com.member.service;

import com.member.constant.Plan;
import com.member.dto.MemberDto;
import com.member.entity.Member;
import com.member.repository.MemberRepository;
import com.setting.entity.Setting;
import com.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingRepository settingRepository;

    @Transactional
    public Long signUp(MemberDto memberDto) {
        if(memberRepository.findByEmail(memberDto.getEmail()).isPresent()){
            throw new IllegalArgumentException("이미 가입된 이메일 입니다");
        }
        Member member = new Member();
        member.setEmail(memberDto.getEmail());
        member.setPassword(passwordEncoder.encode(memberDto.getPassword()));
        member.setName(memberDto.getName());
        member.setPlan(Plan.FREE);
        member.setBirthYear(memberDto.getBirthYear());
        member.setGender(memberDto.getGender());
        member.setRegion(memberDto.getRegion());

        memberRepository.save(member);

//      회원 테이블 생성 된 후에 설정 기본값으로 집어넣기

        Setting setting = new Setting(member.getId());

        settingRepository.save(setting);


        return member.getId();
    }

    
}
