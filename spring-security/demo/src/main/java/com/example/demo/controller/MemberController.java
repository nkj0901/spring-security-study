package com.example.demo.controller;

import com.example.demo.User.Member;
import com.example.demo.dto.JwtToken;
import com.example.demo.dto.LoginDto;
import com.example.demo.service.MemberService;
import com.example.demo.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

//    @PostMapping("/sign-in")
//    public JwtToken signIn(@RequestBody LoginDto dto) {
//        JwtToken jwtToken = memberService.signIn(dto.getUsername(), dto.getPassword());
//        log.info("request username = {}, password = {}", dto.getUsername(), dto.getPassword());
//        log.info("jwtToken accessToken = {}, refreshToken = {}", jwtToken.getAccessToken(), jwtToken.getRefreshToken());
//        return jwtToken;
//    }

    @GetMapping
    public String getUsername(@AuthenticationPrincipal UserDetails user){
        return user.getUsername();
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest request) {
        //request, response가 service 계층으로 넘어가는 것은 좋지 않다. request, response는
        //컨트롤러에서 사용되는 객체이며 Service 계층이 request와 response를 알 필요가 없다.
        //서비스 계층은 비즈니스 로직을 처리하고 데이터 베이스와 상호작용하기 위한 역할을 담당하기 때문에
        //비즈니스 로직에 필요한 데이터만 받도록 해야 한다.
        String refreshToken = jwtTokenProvider.resolveRefreshToken(request);
        String accessToken = jwtTokenProvider.resolveAccessToken(request);
        memberService.logout(refreshToken, accessToken);
    }

    @GetMapping("/reissue")
    public String reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtTokenProvider.resolveRefreshToken(request);
        String newAccessToken = memberService.reissue(refreshToken);
        jwtTokenProvider.accessTokenSetHeader(newAccessToken, response);
        log.info("accessToken 재발급 완료~!");
        return "accessToken 재발급 완료~! Header를 확인해보라";
    }
}
