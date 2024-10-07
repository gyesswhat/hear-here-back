package com.example.hearhere.security.jwt;

public enum TokenErrorResult {
    INVALID_TOKEN("유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN("토큰이 만료되었습니다.");
    // 다른 에러 타입 추가 가능

    private final String message;

    TokenErrorResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
