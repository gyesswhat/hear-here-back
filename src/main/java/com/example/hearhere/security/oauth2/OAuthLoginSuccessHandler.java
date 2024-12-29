package com.example.hearhere.security.oauth2;

import com.example.hearhere.entity.RefreshToken;
import com.example.hearhere.entity.User;
import com.example.hearhere.repository.RefreshTokenRepository;
import com.example.hearhere.repository.UserRepository;
import com.example.hearhere.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Value("${jwt.redirect.local}")
    private String REDIRECT_URI_LOCAL;

    @Value("${jwt.redirect.prod}")
    private String REDIRECT_URI_PROD;

    @Value("${jwt.access-token.expiration-time}")
    private long ACCESS_TOKEN_EXPIRATION_TIME; // 액세스 토큰 유효기간

    @Value("${jwt.refresh-token.expiration-time}")
    private long REFRESH_TOKEN_EXPIRATION_TIME; // 리프레쉬 토큰 유효기간

    private OAuth2UserInfo oAuth2UserInfo = null;

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication; // 토큰
        final String provider = token.getAuthorizedClientRegistrationId(); // provider 추출

        // 구글 || 카카오 || 네이버 로그인 요청
        switch (provider) {
            case "google" -> {
                log.info("구글 로그인 요청");
                oAuth2UserInfo = new GoogleUserInfo(token.getPrincipal().getAttributes());
            }
            case "kakao" -> {
                log.info("카카오 로그인 요청");
                oAuth2UserInfo = new KakaoUserInfo(token.getPrincipal().getAttributes());
            }
            case "naver" -> {
                log.info("네이버 로그인 요청");
                oAuth2UserInfo = new NaverUserInfo((Map<String, Object>) token.getPrincipal().getAttributes().get("response"));
            }
        }

        // 정보 추출
        String providerId = oAuth2UserInfo.getProviderId();
        String name = oAuth2UserInfo.getName();

        User existUser = userRepository.findByProviderId(providerId);
        User user;

        if (existUser == null) {
            // 신규 유저인 경우
            log.info("신규 유저입니다. 등록을 진행합니다.");

            user = User.builder()
                    .userId(UUID.randomUUID())
                    .name(name)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userRepository.save(user); // 등록
        } else {
            // 기존 유저인 경우
            log.info("기존 유저입니다.");
            refreshTokenRepository.deleteByUserId(existUser.getUserId()); // 기존 리프레쉬토큰 삭제
            user = existUser;
        }

        log.info("유저 이름 : {}", name);
        log.info("PROVIDER : {}", provider);
        log.info("PROVIDER_ID : {}", providerId);

        // 리프레쉬 토큰 새로 발급 후 저장
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), REFRESH_TOKEN_EXPIRATION_TIME);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getUserId())
                .token(refreshToken)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // 액세스 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), ACCESS_TOKEN_EXPIRATION_TIME);

        // 아이디, 이름, 액세스 토큰, 리프레쉬 토큰을 담아 리다이렉트
        String encodedName = URLEncoder.encode(name, "UTF-8"); // 사용자 이름 URL엔코딩

        // Host 헤더 기반으로 리다이렉트 URI 결정
        String host = request.getHeader("Host");
        log.info("Host: {}", host);

        String redirectUri;
        if (host != null && (host.contains("localhost") || host.startsWith("127.0.0.1"))) {
            redirectUri = REDIRECT_URI_LOCAL;
        } else {
            redirectUri = REDIRECT_URI_PROD;
        }

        String formattedRedirectUri = String.format(redirectUri, user.getUserId(), encodedName, accessToken, refreshToken);
        getRedirectStrategy().sendRedirect(request, response, formattedRedirectUri);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("SecurityContextHolder 정보: {}", SecurityContextHolder.getContext().getAuthentication());
    }
}
