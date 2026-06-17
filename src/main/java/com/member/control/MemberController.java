package com.member.control;

import com.member.dto.MemberDto;
import com.member.service.MemberService;
import com.security.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/members")
@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/signup")
    public ApiResponse<Long> signup(@RequestBody MemberDto memberDto) {
        Long id= memberService.signUp(memberDto);
        return ApiResponse.success(id);
    }

}
