package com.example.demo.controller;

import com.example.demo.dto.JwtToken;
import com.example.demo.dto.LoginReqDto;
import com.example.demo.service.MemberServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberServiceImpl memberService;

    @PostMapping("/sign-in")
    public JwtToken signIn(@RequestBody LoginReqDto dto) {
        JwtToken jwtToken = memberService.signIn(dto.getUsername(), dto.getPassword());
        log.info("request username = {}, password = {}", dto.getUsername(), dto.getPassword());
        log.info("jwtToken accessToken = {}, refreshToken = {}", jwtToken.getAccessToken(), jwtToken.getRefreshToken());
        return jwtToken;
    }

    @GetMapping("/logout")
    public void logout() {

    }
}
