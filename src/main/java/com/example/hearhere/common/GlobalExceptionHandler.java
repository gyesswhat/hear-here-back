package com.example.hearhere.common;

import com.example.hearhere.common.code.BaseErrorCode;
import com.example.hearhere.dto.ErrorReasonDto;
import com.example.hearhere.security.jwt.TokenErrorResult;
import com.example.hearhere.security.jwt.TokenException;
import com.example.hearhere.security.jwt.UserErrorResult;
import com.example.hearhere.security.jwt.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ApiResponse<BaseErrorCode>> handleTokenException(TokenException ex) {
        return ApiResponse.onFailure(ex.getTokenErrorResult());
    }
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<BaseErrorCode>> handleUserException(UserException e) {
        UserErrorResult errorResult = e.getUserErrorResult();
        return ApiResponse.onFailure(errorResult);
    }
}
