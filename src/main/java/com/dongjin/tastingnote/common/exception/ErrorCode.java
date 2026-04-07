package com.dongjin.tastingnote.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 올바르지 않습니다"),

    // 401 Unauthorized
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "INVALID_LOGIN", "이메일 또는 비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다. 다시 로그인해주세요"),

    // 403 Forbidden
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "FORBIDDEN_ACCESS", "접근 권한이 없습니다"),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 유저입니다"),
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTE_NOT_FOUND", "존재하지 않는 노트입니다"),
    ALCOHOL_NOT_FOUND(HttpStatus.NOT_FOUND, "ALCOHOL_NOT_FOUND", "존재하지 않는 술입니다"),
    FLAVOR_NOT_FOUND(HttpStatus.NOT_FOUND, "FLAVOR_NOT_FOUND", "존재하지 않는 맛/향입니다"),

    // 409 Conflict
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "NICKNAME_ALREADY_EXISTS", "이미 사용 중인 닉네임입니다"),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "ALREADY_REPORTED", "이미 신고한 노트입니다"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}