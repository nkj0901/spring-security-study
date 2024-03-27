package com.example.demo.service;

import com.example.demo.dto.JwtToken;
import jakarta.servlet.http.HttpServletRequest;

public interface MemberService {

    public JwtToken signIn(String username, String password);
    public JwtToken reissue(String encryptedRefreshToken);
}
