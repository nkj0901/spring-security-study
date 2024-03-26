package com.example.demo.util;

import com.example.demo.dto.JwtToken;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

// 참고 https://sjh9708.tistory.com/170
// 참고 2 https://ironmask43.tistory.com/92
// 토큰의 생성, 유효성 검증을 담당할 Token Provider 생성

//claim은 토큰에 포함되는 데이터의 속성, 토큰의 내용을 설명하는 정보를 제공, 토큰의 본문에 포함되어 있다.
//토큰이 전달할 사용자 정보를 포함. 일반적으로 클레임은 사용자의 식별자, 권한 정보, 토큰의 만료 시간 등을 포함합니다.
@Slf4j
@Component
public class JwtTokenProvider implements InitializingBean {
    private static final String AUTHORITIES_KEY = "auth";

    private Key key;
    private final String secret;
    private final long accessTokenExpTime;
    private final long refreshTokenExpTime;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validation-in-seconds}") long accessTokenExpTime, @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenExpTime
    ) {
        this.secret = secret;
        this.accessTokenExpTime = accessTokenExpTime;
        this.refreshTokenExpTime = refreshTokenExpTime;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //TokenProvider bean 생성 후 생성자로 주입받은 sercret 값을 이용해 암호화 키 생성
        //base64로 인코딩된 시크릿 값을 디코딩하여 바이트 배열로 변환
        //jwt 시크립 값은 일반적으로 Base64되어 있어 보통 디코딩하여 바이트 배열로 변환하는 과정이 필요하다.
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        //시크리 값을 HMAC_SHA 알고리즘에 사용할 수 있는 비밀 키로 변환
        //HMAC-SHA 알고리즘은 JWT 서명(signature)을 생성하거나 검증하는데 사용된다.
        //따라서 jwt의 시크릿 값을 이용하여 HMAC-SHA에 사용할 수 있는 키로 변화하는 과정이 필요하다.
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    //Authentication 객체를 받아서 토큰 생성, 반환
    public JwtToken createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        //토큰 유효시간 설정
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime accessTokenValidity = now.plusSeconds(accessTokenExpTime);
        ZonedDateTime refresgTokenValidity = now.plusSeconds(refreshTokenExpTime);


        //jwt 토큰 생성, 리턴
        String accessToken =  Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(Date.from(accessTokenValidity.toInstant()))
                .compact();
// refresh token 참고 https://suddiyo.tistory.com/entry/Spring-Spring-Security-JWT-%EB%A1%9C%EA%B7%B8%EC%9D%B8-%EA%B5%AC%ED%98%84%ED%95%98%EA%B8%B0-2
        String refreshToken = Jwts.builder()
                .setExpiration(Date.from(refresgTokenValidity.toInstant()))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String token) {

        Claims claims = parseClaims(token);

        if(claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        //권한 추출
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        //각 권한 문자열을 SimpleGrantedAuthority 객체로 매핑
                        //simpleGrantedAuthority Spring Security 사용되는 권한을 나타내는 클래스이다.
                        //이 과정을 통해 jwt에 담겨져 있던 문자열을 가지고 spring Security 사용할 수 있게 됨
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        //UserDetails 객체를 만들어서 Authentication return
        //UserDetails: interface, User: UserDeatails를 구현한 class
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 토큰의 유효성 검사
    public boolean validateToken(String token) {
        try{
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 서명입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    //Access token을 복호화
    private Claims parseClaims(String token) {
        try{
            return  //jwt 파싱 및 검증
                    //Jwts.parserBuilder().setSigningKey(key).build()을 사용하여 JWT 파서를 빌드합니다.
                    //여기서 key는 JWT의 서명을 검증하는 데 사용되는 시크릿 키입니다.
                    Jwts
                    .parserBuilder()
                    .setSigningKey(key)
                    .build()
                    //parseClaimsJws(token)을 호출하여 주어진 JWT 토큰을 파싱하고 검증합니다.
                    //이 때, JWT의 서명이 유효한지 확인하고, 토큰의 내용을 추출합니다.
                    .parseClaimsJws(token)
                    //getBody()를 호출하여 JWT의 본문(Claims)을 가져옵니다.
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

}
