package com.example.dance_community.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400: 잘못된 요청 (직접 던진 예외)
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRequest(InvalidRequestException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    // 400: 유효성 검사 실패 (MethodArgumentNotValidException)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String errorMessage = "유효성 검사 실패";
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError != null) {
            errorMessage = fieldError.getDefaultMessage();
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errorMessage);
        if (fieldError != null) {
            pd.setProperty("field", fieldError.getField());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    // 400 Bad Request: 필수 쿠키 누락 (MissingRequestCookieException)
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ProblemDetail> handleMissingCookie(MissingRequestCookieException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "필수 쿠키가 누락되었습니다: " + ex.getCookieName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    // 401 Unauthorized: 인증 실패 (로그인 실패, 토큰 만료/위조)
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> handleAuthException(AuthException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    // 403 Forbidden: 권한 부족 (관리자 페이지 접근 등)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    // 404: 리소스 찾을 수 없음
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    // 409: 리소스 충돌 (중복 등)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    // 500: 서버 내부 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAll(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
        pd.setProperty("error", ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }
}