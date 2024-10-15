package com.example.hearhere.security;

import com.example.hearhere.common.ApiResponse;
import com.example.hearhere.common.status.ErrorStatus;
import com.example.hearhere.security.jwt.JwtUtil;
import com.example.hearhere.security.jwt.TokenException;
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

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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

            // 토큰에서 userId 추출
            String token = authorizationHeader.substring(7);
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

    // TokenException 처리
    private void handleTokenException(HttpServletResponse response, TokenException e) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // TokenException 메시지를 사용하여 JSON 응답 반환
        String jsonResponse = "{\"error\": \"Invalid Token\", \"message\": \"" + e.getMessage() + "\"}";
        response.getWriter().write(jsonResponse);    }
}
