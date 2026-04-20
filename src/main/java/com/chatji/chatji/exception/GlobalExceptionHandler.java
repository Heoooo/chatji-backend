package com.chatji.chatji.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 외부 API(네이버 등) 통신 에러 처리
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleRestClientResponseException(RestClientResponseException e) {
        log.error("[API-RESPONSE-ERROR] Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse("EXTERNAL_API_ERROR", "외부 API 연동 중 문제가 발생했습니다: " + e.getStatusText()));
    }

    /**
     * 그 외 모든 예상치 못한 에러 처리 (NPE 등)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        // e.getMessage()가 null인 경우를 대비해 예외 타입까지 출력
        log.error("[CRITICAL-ERROR] Type: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다. 로그를 확인해 주세요."));
    }

    public record ErrorResponse(String errorCode, String message) {
    }
}
