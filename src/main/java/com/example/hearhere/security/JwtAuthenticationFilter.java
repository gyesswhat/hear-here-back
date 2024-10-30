package com.example.hearhere.security;

import com.example.hearhere.common.ApiResponse;
import com.example.hearhere.common.status.ErrorStatus;
import com.example.hearhere.security.jwt.JwtUtil;
import com.example.hearhere.security.jwt.TokenException;
import com.example.hearhere.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

import static com.example.hearhere.security.jwt.TokenErrorResult.INVALID_TOKEN;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        try {
            // 토큰이 없거나 형식이 잘못된 경우 TokenException 발생
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new TokenException(INVALID_TOKEN);
            }


            // 토큰 추출
            String token = authorizationHeader.substring(7);

            // 블랙리스트 검증
            if (tokenBlacklistService.isBlacklisted(token)) {
                throw new TokenException(INVALID_TOKEN);
            }

            // 토큰에서 유저id 추출
            String userId = jwtUtil.getUserIdFromToken(token);

            // userId가 null이 아니고 인증되지 않았다면 인증 처리
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // 필터 체인 계속 진행
            filterChain.doFilter(request, response);

        } catch (TokenException e) {
            // TokenException 발생 시 처리
            handleTokenException(response, e);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 토큰 검사를 하지 않을 경로를 설정
        String path = request.getServletPath();

        return path.equals("/login") ||
                path.startsWith("/oauth2/authorization") ||
                path.startsWith("/login/oauth2/code") ||
                path.equals("/reissue/access-token") ||
                path.equals("/asmr/randomprompts") ||
                path.equals("/asmr/generate") ||
                path.equals("/");
    }

    // TokenException 처리
    private void handleTokenException(HttpServletResponse response, TokenException e) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // TokenException 메시지를 사용하여 JSON 응답 반환
        String jsonResponse = "{\"error\": \"Invalid Token\", \"message\": \"" + e.getMessage() + "\"}";
        response.getWriter().write(jsonResponse);    }

}
