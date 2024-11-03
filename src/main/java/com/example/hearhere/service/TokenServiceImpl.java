package com.example.hearhere.service;

import com.example.hearhere.entity.RefreshToken;
import com.example.hearhere.repository.RefreshTokenRepository;
import com.example.hearhere.security.jwt.JwtUtil;
import com.example.hearhere.security.jwt.TokenErrorResult;
import com.example.hearhere.security.jwt.TokenException;
import com.example.hearhere.security.jwt.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    @Value("${jwt.access-token.expiration-time}")
    private long ACCESS_TOKEN_EXPIRATION_TIME; // 액세스 토큰 유효기간

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Override
    public TokenResponse reissueAccessToken(String authorizationHeader) {
        String refreshToken = jwtUtil.getTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.getUserIdFromToken(refreshToken);

        // 1. 리프레시 토큰을 데이터베이스에서 조회하고 존재 여부 확인
        RefreshToken existRefreshToken = refreshTokenRepository.findByUserId(UUID.fromString(userId));
        if (existRefreshToken == null) {
            throw new TokenException(TokenErrorResult.INVALID_REFRESH_TOKEN); // 리프레시 토큰이 없으면 예외 발생
        }

        // 2. 리프레시 토큰이 다르거나 만료된 경우 예외 발생
        if (!existRefreshToken.getToken().equals(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            throw new TokenException(TokenErrorResult.INVALID_REFRESH_TOKEN);
        }

        // 3. 유효한 리프레시 토큰인 경우 액세스 토큰 재발급
        String accessToken = jwtUtil.generateAccessToken(UUID.fromString(userId), ACCESS_TOKEN_EXPIRATION_TIME);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    @Override
    public String findUserIdByToken(String authorizationHeader) {
        String accessToken = jwtUtil.getTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        return userId;
    }
}
