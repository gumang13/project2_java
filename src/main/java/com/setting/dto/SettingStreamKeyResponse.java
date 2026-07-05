// 목적: 휴대폰 RTMP 회원별 스트림 키 조회 응답 DTO.
//       회원 이메일에서 만든 유일 키와, 내부(FastAPI Pull)·외부(사용자 폰 Push) RTMP 주소를 담는다.
package com.setting.dto;

public record SettingStreamKeyResponse(
        String key,       // 회원별 유일 스트림 키 (이메일 기반)
        String pullUrl,   // FastAPI가 당겨오는 내부 RTMP 주소
        String pushUrl    // 사용자가 휴대폰 앱에 입력하는 외부 RTMP 주소
) {
}
