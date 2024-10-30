package com.example.hearhere.common;

import com.example.hearhere.common.code.BaseCode;
import com.example.hearhere.common.code.BaseErrorCode;
import com.example.hearhere.dto.ReasonDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {
    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T payload;
    public static <T> ResponseEntity<ApiResponse<T>> onSuccess(BaseCode code, T data) {
        ReasonDto reason = code.getReasonHttpStatus();
        ApiResponse<T> response = new ApiResponse<>(true, reason.getCode(), reason.getMessage(), data);
        return ResponseEntity.status(reason.getHttpStatus()).body(response);
    }

    public static <T> ResponseEntity<ApiResponse<T>> onFailure(BaseErrorCode code) {
        ApiResponse<T> response = new ApiResponse<>(false, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), null);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }
}
