package com.example.hearhere.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 액세스 토큰을 발급하는 메서드
    public String generateAccessToken(UUID userId, long expirationMillis) {
        String token = Jwts.builder()
                .claim("userId", userId.toString()) // 클레임에 userId 추가
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis)) // 만료 시간
                .signWith(this.getSigningKey()) // 서명
                .compact();

        log.info("발급된 액세스 토큰: {}", token);  // 액세스 토큰 출력
        return token;
    }

    // 리프레쉬 토큰을 발급하는 메서드
    public String generateRefreshToken(UUID userId, long expirationMillis) {
        String token = Jwts.builder()
                .claim("userId", userId.toString()) // 클레임에 userId 추가
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis)) // 만료 시간
                .signWith(this.getSigningKey()) // 서명
                .compact();

        log.info("발급된 리프레쉬 토큰: {}", token);  // 리프레쉬 토큰 출력
        return token;
    }

    // 응답 헤더에서 액세스 토큰을 반환하는 메서드
    public String getTokenFromHeader(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }

    // 토큰에서 유저 id를 반환하는 메서드
    public String getUserIdFromToken(String token) {
        try {
            log.info("검증하려는 토큰: {}", token);  // 파싱 전 토큰 확인
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.getSigningKey())  // 서명 키 설정
                    .build()
                    .parseClaimsJws(token)  // JWT 토큰 파싱
                    .getBody();
            log.info("유효한 토큰입니다. 유저 id를 반환합니다: {}", claims.get("userId", String.class));
            return claims.get("userId", String.class);  // 유저 ID 반환
        } catch (JwtException e) {
            log.error("JWT 토큰 서명 검증 실패: {}", e.getMessage());  // 서명 검증 실패 시 로그 출력
            throw new TokenException(TokenErrorResult.INVALID_TOKEN);  // 예외 발생
        }
    }

    // Jwt 토큰의 유효기간을 확인하는 메서드
    public boolean isTokenExpired(String token) {
        try {
            Date expirationDate = Jwts.parserBuilder() // 수정된 부분
                    .setSigningKey(this.getSigningKey()) // 수정된 부분
                    .build()
                    .parseClaimsJws(token) // 수정된 부분
                    .getBody()
                    .getExpiration();
            log.info("토큰의 유효기간을 확인합니다.");
            return expirationDate.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않은 경우
            log.warn("유효하지 않은 토큰입니다.");
            throw new TokenException(TokenErrorResult.INVALID_TOKEN);
        }
    }
}
