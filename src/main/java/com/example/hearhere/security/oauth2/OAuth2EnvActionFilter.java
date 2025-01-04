package com.example.hearhere.security.oauth2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2EnvActionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/oauth2/authorization/")) {
            String env = request.getParameter("env");
            if (env != null) {
                log.info("env 파라미터 감지: {}", env);

                // env 값을 세션에 저장
                request.getSession().setAttribute("env", env);
                log.info("env 값 세션에 저장 완료: {}", env);
            }

            String action = request.getParameter("action");
            if (action != null) {
                log.info("action 파라미터 감지: {}", action);

                // action 값을 세션에 저장
                request.getSession().setAttribute("action", action);
                log.info("action 값 세션에 저장 완료: {}", action);
            }
        }
        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
