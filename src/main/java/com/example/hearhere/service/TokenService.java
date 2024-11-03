package com.example.hearhere.service;

import com.example.hearhere.security.jwt.TokenResponse;

public interface TokenService {
    TokenResponse reissueAccessToken(String authorizationHeader);
    public String findUserIdByToken(String authorizationHeader);
}
