package com.example.demo.service;

import com.example.demo.User.Member;
import com.example.demo.User.MemberRepository;
import com.example.demo.dto.JwtToken;
import com.example.demo.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService{
    private final MemberRepository memberRepotory;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RedisService redisService;

// UsernamePasswordAuthenticationFilter 안 쓸 때 쓰던 메소드

//    @Override
//    public JwtToken signIn(String username, String password) {
//        //1.username + password를 기반으로 Authentication 객체 생성
//        //이때 authentication은 인증 여부를 확인하는 authenticated 값이 false
//        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
//
//        //2. 실제 검증 authenticate() 메서드를 통해 요청된 Member에 대한 검증 진행
//        //authenticate 메서드가 실행될 때 CustomUserDetailsService에서 만든 loadUserByUsername메서드 실행
//        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
//
//        //3, 인증 정보를 기반으로 jwt 트큰 생성
//        JwtToken jwtToken = jwtTokenProvider.createToken(authentication);
//
//        return jwtToken;
//    }

    @Override
    public String reissue(String refreshToken) {
        //refreshToken이 비어있는지 확인
        verifiedRefreshToken(refreshToken);
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String name = claims.getSubject();
        String redisRefreshToken = redisService.getValues(name);

        if(redisService.checkExistsValue(redisRefreshToken) && refreshToken.equals(redisRefreshToken)){
            Member findMember = this.findMemberByName(name);
            JwtToken jwtToken = jwtTokenProvider.createToken(findMember);
            String newAccessToken = jwtToken.getAccessToken();
            return newAccessToken;
        } else {
            log.info("refresh token 토큰 일치하지 않음");
            return null;
        }
    }

    @Override
    public void logout(String refreshToken, String accessToken) {
        verifiedRefreshToken(refreshToken);
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String username = claims.getSubject();
        String redisRefreshToken = redisService.getValues(username);
        if(redisService.checkExistsValue(redisRefreshToken)){
            redisService.deleteValues(username);

            //로그아웃 시 Access Token Redis 저장( key = accessToken / value = " logout"
            long accessTokenExpIn = jwtTokenProvider.getAccessTokenExpTime();
            redisService.setValues(accessToken, "logout", Duration.ofMillis(accessTokenExpIn));
        }
    }

    private void verifiedRefreshToken(String refreshToken) {
        if(refreshToken == null) {
            log.info("encryptedRefreshToken이 없습니다.");
        }
    }

    private Member findMemberByName(String name) {
        return memberRepotory.findByUsername(name).orElseThrow(() ->{
            try {
                throw new Exception();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
