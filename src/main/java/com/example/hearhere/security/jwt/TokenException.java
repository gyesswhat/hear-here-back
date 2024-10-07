package com.example.hearhere.security.jwt;

public class TokenException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final TokenErrorResult errorResult;

    public TokenException(TokenErrorResult errorResult) {
        super(errorResult.getMessage());
        this.errorResult = errorResult;
    }

    public TokenErrorResult getErrorResult() {
        return errorResult;
    }
}
