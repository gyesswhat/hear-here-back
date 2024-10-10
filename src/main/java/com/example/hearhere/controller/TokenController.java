package com.example.hearhere.controller;

import com.example.hearhere.common.ApiResponse;
import com.example.hearhere.common.status.SuccessStatus;
import com.example.hearhere.security.jwt.TokenResponse;
import com.example.hearhere.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TokenController {
    private final TokenService authService;

    // 액세스 토큰을 재발행하는 API, 리프레쉬토큰 받아서 검증
    @GetMapping("/reissue/access-token")
    public ResponseEntity<ApiResponse<Object>> reissueAccessToken(
            @RequestHeader("Authorization") String authorizationHeader) {

        TokenResponse accessToken = authService.reissueAccessToken(authorizationHeader);
        return ApiResponse.onSuccess(SuccessStatus._CREATED_ACCESS_TOKEN, accessToken);
    }
}
