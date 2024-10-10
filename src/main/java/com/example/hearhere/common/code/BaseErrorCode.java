package com.example.hearhere.common.code;

import com.example.hearhere.dto.ErrorReasonDto;

public interface BaseErrorCode {
    public ErrorReasonDto getReason();

    public ErrorReasonDto getReasonHttpStatus();
}
