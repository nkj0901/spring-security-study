package com.example.demo.util;

import com.example.demo.dto.JwtToken;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
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
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

// 참고 https://sjh9708.tistory.com/170
// 참고 2 https://ironmask43.tistory.com/92
// 참고 2 https://green-bin.tistory.com/68?category=1116728

// 토큰의 생성, 유효성 검증을 담당할 Token Provider 생성

//claim은 토큰에 포함되는 데이터의 속성, 토큰의 내용을 설명하는 정보를 제공, 토큰의 본문에 포함되어 있다.
//토큰이 전달할 사용자 정보를 포함. 일반적으로 클레임은 사용자의 식별자, 권한 정보, 토큰의 만료 시간 등을 포함합니다.
@Slf4j
@Component
public class JwtTokenProvider implements InitializingBean {
    private static final String BEARER_TYPE = "Bearer";
    private static final String BEARER_PREFIX = "Bearer";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String REFRESH_HEADER = "Refresh";
    private static final String AUTHORITIES_KEY = "role";

    @Getter
    @Value("${jwt.secret}")
    public String secret;

    @Getter
    @Value("${jwt.access-token-validation-in-seconds}")
    public long accessTokenExpTime;

    @Getter
    @Value("${jwt.refresh-token-validity-in-seconds}")
    public long refreshTokenExpTime;
    public Key key;


    @Override
    public void afterPropertiesSet() throws Exception {
        //TokenProvider bean 생성 후 sercret 값을 이용해 암호화 키 생성
        //base64로 인코딩된 시크릿 값을 디코딩하여 바이트 배열로 변환
        //jwt 시크립 값은 일반적으로 Base64되어 있어 보통 디코딩하여 바이트 배열로 변환하는 과정이 필요하다.
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        //시크릿 값을 HMAC_SHA 알고리즘에 사용할 수 있는 비밀 키로 변환
        //HMAC-SHA 알고리즘은 JWT 서명(signature)을 생성하거나 검증하는데 사용된다.
        //따라서 jwt의 시크릿 값을 이용하여 HMAC-SHA에 사용할 수 있는 키로 변화하는 과정이 필요하다.
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    //Authentication 객체를 받아서 토큰 생성, 반환
    public JwtToken createToken(UserDetails userDetails) {
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        //토큰 유효시간 설정
        Date accessTokenExpiresIn = getTokenExpiration(accessTokenExpTime);
        Date refreshTokenExpiresIn = getTokenExpiration(refreshTokenExpTime);


        //jwt 토큰 생성, 리턴
        String accessToken =  Jwts.builder()
                .claim(AUTHORITIES_KEY, authorities)
                .setSubject(userDetails.getUsername())
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(accessTokenExpiresIn)
                .compact();
// refresh token 참고 https://suddiyo.tistory.com/entry/Spring-Spring-Security-JWT-%EB%A1%9C%EA%B7%B8%EC%9D%B8-%EA%B5%AC%ED%98%84%ED%95%98%EA%B8%B0-2
        // setClaimms() : jwt에 포함시킬 Custom Claims를 추가한다. Custom Claims는 주로 인증된 사용자 정보를 넣는다.
        // setSubject() : jwt에 대한 제목을 넣는다.
        // setIssuedAt() : jwt의 발행 일자를 넣는다. 파라미터 타입은 java.util.Date 타입니다.
        // setExpiration() : jwt의 만료기한을 지정한다. 파라미터 타입은 java.util.Date 타입이다.
        // signWith() :  서명을 위한 Key(java.security.Key) 객체를 설정한다.
        // compact() : jwt를 생성하고 직렬화한다.
        String refreshToken = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setExpiration(refreshTokenExpiresIn)
                //민감한게 아니라서 HS256 생략
                .signWith(key)
                .compact();

        return JwtToken.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .date(accessTokenExpiresIn)
                .build();
    }

    // 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String token) {

        Claims claims = parseClaims(token);

        if(claims.get(AUTHORITIES_KEY) == null) {
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
        log.info("# AuthMember.getRoles 권한 체크 = {}", principal.getAuthorities().toString());
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 토큰의 유효성 검사
    public boolean validateToken(String token) {
        try{
            parseClaims(token);
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

    //token을 복호화
    public Claims parseClaims(String token) {
        return  //jwt 파싱 및 검증
                //Jwts.parserBuilder().setSigningKey(key).build()을 사용하여 JWT 파서를 빌드합니다.
                //여기서 key는 JWT의 서명을 검증하는 데 사용되는 시크릿 키입니다.
                Jwts
                .parserBuilder()
                //어떤 알고리즘 쓰는지는 이미 JWT 토큰에 포함되어 있어서 별도로 지정할 필요가 없음
                .setSigningKey(key)
                .build()
                //parseClaimsJws(token)을 호출하여 주어진 JWT 토큰을 파싱하고 검증합니다.
                //이 때, JWT의 서명이 유효한지 확인하고, 토큰의 내용을 추출합니다.
                .parseClaimsJws(token)
                //getBody()를 호출하여 JWT의 본문(Claims)을 가져옵니다.
                .getBody();
    }

    public void accessTokenSetHeader(String accessToken, HttpServletResponse response) {
        String headerValue = BEARER_PREFIX + accessToken;
        response.setHeader(AUTHORIZATION_HEADER, headerValue);
    }

    public void refreshTokenSetHeader(String refreshToken, HttpServletResponse response) {
        response.setHeader("Refresh", refreshToken);
    }

    // Request Header에 Access Token 정보를 추출하는 메서드
    public String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(REFRESH_HEADER);
        if(StringUtils.hasText(bearerToken)) return bearerToken;
        else log.info("refresh 토큰이 없음");
        return null;
    }

    private Date getTokenExpiration(long expTime) {
        Date date = new Date();
        return new Date(date.getTime() + expTime);
    }
}
