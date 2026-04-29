package com.dongjin.tastingnote.common.response;

import com.dongjin.tastingnote.common.exception.ErrorCode;

public record ErrorResponse(boolean success, String errorCode, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage());
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, "INVALID_INPUT", message);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.getCode(), message);
    }
}