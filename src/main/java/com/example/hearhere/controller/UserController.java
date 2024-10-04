package com.example.hearhere.controller;

import com.example.hearhere.dto.LoginRequestDto;
import com.example.hearhere.dto.LoginResponseDto;
import com.example.hearhere.dto.SignedupRequestDto;
import com.example.hearhere.entity.Role;
import com.example.hearhere.entity.User;
import com.example.hearhere.security.jwt.JwtTokenProvider;
import com.example.hearhere.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;


    public UserController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignedupRequestDto signupRequest) {
        // 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

        // 새 사용자 생성
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setPassword(encodedPassword);

        // 데이터베이스에서 기본 역할 조회 (ROLE_USER)
        Role roleUser = userService.getRoleByName("ROLE_USER");

        user.setRoles(Collections.singleton(roleUser));  // 한 명의 사용자가 여러 개의 역할을 가질 수 있으므로 Set으로 저장

        // 사용자 저장
        userService.saveUser(user);

        // 바로 로그인 처리
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signupRequest.getUsername(), signupRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성 및 반환
        String token = jwtTokenProvider.createToken(authentication);
        LoginResponseDto loginResponse = new LoginResponseDto(token);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성 및 반환
        String token = jwtTokenProvider.createToken(authentication);
        LoginResponseDto loginResponse = new LoginResponseDto(token);

        return ResponseEntity.ok(loginResponse);
    }
}
