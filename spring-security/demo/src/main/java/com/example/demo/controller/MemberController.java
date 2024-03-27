package com.example.demo.controller;

import com.example.demo.dto.JwtToken;
import com.example.demo.dto.LoginDto;
import com.example.demo.service.MemberService;
import com.example.demo.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/sign-in")
    public JwtToken signIn(@RequestBody LoginDto dto) {
        JwtToken jwtToken = memberService.signIn(dto.getUsername(), dto.getPassword());
        log.info("request username = {}, password = {}", dto.getUsername(), dto.getPassword());
        log.info("jwtToken accessToken = {}, refreshToken = {}", jwtToken.getAccessToken(), jwtToken.getRefreshToken());
        return jwtToken;
    }

    @GetMapping("/logout")
    public void logout() {

    }

    @GetMapping("/reissue")
    public JwtToken reissue(HttpServletRequest request, HttpServletResponse response) {
        String encryptedRefreshToken = jwtTokenProvider.resolveRefreshToken(request);
        log.info("encryptedRefreshToken {}", encryptedRefreshToken);
        memberService.reissue(encryptedRefreshToken);
        return null;
    }
}
