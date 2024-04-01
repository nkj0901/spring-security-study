package com.example.demo.filter;

import com.example.demo.service.RedisService;
import com.example.demo.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Jwt를 검증 후 성공하면 SecurityContext에 저장하는 필터. OncePerRequestFilter를 상속받고 있다.
 * OncePerRequestFilter는 각 HTTP 요청에 한 번만 실행되는 것을 보장한다. H
 * HTTP 요청마다 JWT 검증하는것은 비효율적이기 때문에
 * OncePerRequestFilter를 상속함으로써 jwt 검증을 보다 효율적으로 수행할 수 있다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtVerificationFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDE_URL =
            List.of("/"
                , "/h2"
                , "/members/sign-in"
                , "/member/reissue"
                , "/favicon.ico");

    //인증에서
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    // JWT 인증 정보를 현재 쓰레드의 SecurityContext에 저장(가입/로그인/재발급 Request 제외)
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("지금 JwtVerificationFilter 실행중 {}", request.getRequestURI());
        String accessToken = jwtTokenProvider.resolveAccessToken(request);
        if(StringUtils.hasText(accessToken) && doNotLogout(accessToken) &&
        jwtTokenProvider.validateToken(accessToken)) {
            setAuthenticationToContext(accessToken);
        } else {
            log.info("JwtVerificationFilter 실패");
        }
        filterChain.doFilter(request, response);
    }

    private boolean doNotLogout(String accessToken) {
        String isLogout = redisService.getValues(accessToken);
        log.info("doNotLogout 확인 : {}", isLogout);
        return isLogout.equals("false");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        boolean result = EXCLUDE_URL.stream().anyMatch(exclude -> exclude.equalsIgnoreCase(request.getServletPath()));
        return result;
    }

    private void setAuthenticationToContext(String accessToken) {
        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("# Token verification success!");
    }
}
