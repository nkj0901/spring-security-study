package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Slf4j
public class CustomAuthorityUtils {

    // role 값을 기반으로 권한 정보를 생성하여 List<GrantedAuthority> 타입으로 변환한다.
    public static List<GrantedAuthority> createAuthorities(String role) {
        return List.of(new SimpleGrantedAuthority(role));
    }
}
