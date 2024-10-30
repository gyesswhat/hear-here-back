package com.example.hearhere.controller;

import com.example.hearhere.common.ApiResponse;
import com.example.hearhere.common.status.SuccessStatus;
import com.example.hearhere.repository.RefreshTokenRepository;
import com.example.hearhere.security.jwt.JwtUtil;
import com.example.hearhere.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LogoutController {

    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        // Authorization 헤더에서 토큰 추출
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String accesstoken = jwtUtil.getTokenFromHeader(authorizationHeader);

            // Redis에 블랙리스트로 추가하여 무효화
            tokenBlacklistService.addToBlacklist(accesstoken);

            // 액세스 토큰에서 userId 추출
            UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(accesstoken));

            // 해당 사용자의 리프레시 토큰을 데이터베이스에서 삭제하여 무효화
            refreshTokenRepository.deleteByUserId(userId);
        }

        // SecurityContext에서 인증 정보 제거
        SecurityContextHolder.clearContext();

        return ApiResponse.onSuccess(SuccessStatus._OK, null);  // 로그아웃 성공 응답 반환
    }
}
