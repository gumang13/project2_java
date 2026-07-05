// 목적: 로그인 회원의 이메일로 휴대폰 RTMP 격리용 "회원별 유일 스트림 키"와
//       내부(FastAPI Pull)·외부(사용자 Push) RTMP 주소를 생성한다.
//       공용 /live/phone 키 충돌(여러 사용자 영상 섞임)을 막기 위함.
package com.setting.service;

import com.member.entity.Member;
import com.member.repository.MemberRepository;
import com.setting.dto.SettingStreamKeyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingStreamKeyService {

    private final MemberRepository memberRepository;

    // RTMP 베이스 주소 (배포 환경 기준; 필요 시 application.properties로 외부화 가능)
    private static final String PULL_BASE = "rtmp://nginx-rtmp:1935/live/";        // 내부: FastAPI가 당겨오는 주소
    private static final String PUSH_BASE = "rtmp://211.176.114.107:12342/live/";  // 외부: 사용자가 폰 앱에 입력하는 주소

    // 로그인 회원의 이메일로 유일 스트림 키를 만들고, 내부/외부 RTMP 주소와 함께 반환한다.
    public SettingStreamKeyResponse getStreamKey(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        String key = toStreamKey(member.getEmail());
        return new SettingStreamKeyResponse(key, PULL_BASE + key, PUSH_BASE + key);
    }

    // 이메일을 URL/파일명에 안전한 스트림 키로 변환한다 (영소문자·숫자·_ 만 남김).
    // ex) ckehgjs79@naver.com -> ckehgjs79_naver_com
    private String toStreamKey(String email) {
        String lower = (email == null) ? "" : email.trim().toLowerCase();
        return lower.replaceAll("[^a-z0-9]", "_");
    }
}
