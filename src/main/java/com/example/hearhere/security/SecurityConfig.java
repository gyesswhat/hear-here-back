package com.example.hearhere.security;

import com.example.hearhere.security.jwt.JwtUtil;
import com.example.hearhere.security.oauth2.OAuth2EnvActionFilter;
import com.example.hearhere.security.oauth2.OAuthLoginFailureHandler;
import com.example.hearhere.security.oauth2.OAuthLoginSuccessHandler;
import com.example.hearhere.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
    private final OAuthLoginFailureHandler oAuthLoginFailureHandler;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setAllowedOriginPatterns(Arrays.asList(
                "https://hearhere-front.vercel.app/",
                "http://localhost:5173",
                "https://hearhere-back.site")); // 허용할 origin
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/").permitAll()
                                .requestMatchers("/login", "/oauth2/authorization/**", "/login/oauth2/code/**", "/reissue/access-token").permitAll() // 로그인 및 OAuth 경로는 모두 허용
                                .requestMatchers("/logout").permitAll()
                                .requestMatchers("/asmr/randomprompts").permitAll()
                                .requestMatchers("/asmr/generate").permitAll()
                                .requestMatchers("/callback/suno").permitAll()
                                .anyRequest().authenticated() // 그 외 요청은 인증 필요
                )
                .logout(logout -> logout.disable())
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, tokenBlacklistService), UsernamePasswordAuthenticationFilter.class)  // JWT 필터 추가
                .addFilterBefore(new OAuth2EnvActionFilter(), OAuth2AuthorizationRequestRedirectFilter.class)
                .oauth2Login(oauth ->
                        oauth
                                .successHandler(oAuthLoginSuccessHandler) // 로그인 성공 시 핸들러
                                .failureHandler(oAuthLoginFailureHandler) // 로그인 실패 시 핸들러
                )
        ;

        return http.build();
    }
}
