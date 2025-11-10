package com.tguard.tguard_backend.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 정보를 찾을 수 없습니다."),
    INVALID_TRANSACTION(HttpStatus.BAD_REQUEST, "유효하지 않은 거래입니다."),

    DETECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이상 거래 탐지에 실패했습니다."),
    DETECTION_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "탐지 결과를 찾을 수 없습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
