package com.example.demo.filter;

import com.example.demo.dto.JwtToken;
import com.example.demo.dto.LoginDto;
import com.example.demo.service.RedisService;
import com.example.demo.util.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;

    //토큰 헤더에 입력시 설정한 key 값
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /*
    attemptAuthentication() : 인증을 시도하는 메서드.
    HttpServletRequest와 HttpServletResponse를 배개변수로 받아
    사용자가 입력한 로그인 정보를 추출하고 Authentication 객체를 반환하고
    인증이 실패하면 AuthenticationException을 throw한다.
    */
    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.info("지금 attemptAuthentication 실행중");
//        ServletInputStream을 LoginDto 객체로 역직렬화
        ObjectMapper objectMapper = new ObjectMapper();
        LoginDto loginDto = objectMapper.readValue(request.getInputStream(), LoginDto.class);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getParameter("username"), request.getParameter("password"));
// login 메소드의 Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken); '
// 인증 부분에서 암호화된 비밀번호와 사용자 입력 비밀번호의 비교가 가능하게 됩니다.
        return authenticationManagerBuilder.getObject().authenticate(authenticationToken);
    }

    /*
    successfulAuthentication() : 인증이 성공했을 때 호출되는 메서드이다.
    인증 성공 후에 응답을 한다거나 인증정보를 저장하는 등의 추가작업을 할 수 있다.
    아래 코드에서는 redis에 refreshToken 정보를 저장하고 잇다.
    */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        log.info("지금 successfulAuthentication 실행중");
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        JwtToken jwtToken = jwtTokenProvider.createToken(userDetails);
        jwtTokenProvider.accessTokenSetHeader(jwtToken.getAccessToken(), response);
        jwtTokenProvider.refreshTokenSetHeader(jwtToken.getRefreshToken(), response);

        long refreshTokenExpTime = jwtToken.getAccessTokenExpiresIn();
        redisService.setValues(authentication.getName(), jwtToken.getRefreshToken(), Duration.ofMillis(refreshTokenExpTime));
    }

//UsernamePasswordAuthenticationFilter 사용안했을 때 코드

//    //토큰의 인증정보를 security Context에 저장하는 역할을 수행
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
//        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
//        //Request Header에서 JWT 토큰 추출
//        String jwt = resolveToken(httpServletRequest);
//        String requestURI = httpServletRequest.getRequestURI();
//
//        //validateToken을 통해 토큰의 유효성 검사, 정상이면 토큰을 Security Context에 저장
//        if(jwt != null && jwtTokenProvider.validateToken(jwt)) {
//            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//            log.info("Security Context에 '{}'인증정보를 저장했습니다. uri:{}", authentication.getName(), requestURI);
//        } else {
//            log.info("jwt 토큰이 없습니다. {}", requestURI);
//        }
//        filterChain.doFilter(servletRequest, servletResponse);
//    }
//
//    private String resolveToken(HttpServletRequest request) {
//        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
//        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")){
//            return bearerToken.substring(7);
//        }
//        return null;
//    }
}
