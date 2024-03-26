package com.example.demo.config;

import com.example.demo.error.JwtAccessDeniedHandler;
import com.example.demo.error.JwtAuthenticationEntryPoint;
import com.example.demo.User.MemberRepository;
import com.example.demo.filter.JwtFilter;
import com.example.demo.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Slf4j
@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;


    //SecurityFilterChain을 Bean으로 등록하는 과정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //token을 사용하는 방식이기 때문에 csrf를 disable합니다.
                .csrf(csrf -> csrf.disable())
                //참고 코드 https://velog.io/@goat_hoon/Spring-Security%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%9C-JWT-%EB%8F%84%EC%9E%85%EA%B8%B0
                .formLogin((formLogin) -> formLogin
                        .loginPage("/login").defaultSuccessUrl("/", true))
                .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                        .requestMatchers("/", "login/**","/members/sign-in","img/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api1").hasRole("user")
                        .requestMatchers("/api2").hasRole("admin")
                        .requestMatchers("/user/**").hasRole("user")
                        .anyRequest().authenticated())
                // 컨트롤러의 예외처리를 담당하는 exception handler랑은 다름
                .exceptionHandling((exceptionHandling) -> exceptionHandling
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html#servlet-headers-frame-options
                .headers(headers ->
                        headers.xssProtection(
                        xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        ).contentSecurityPolicy(
                        cps -> cps.policyDirectives("script-src 'self'")
                        )
                )
                .addFilterBefore(new JwtFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }

    //보안을 위해 패스워드 암호화시 사용
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
