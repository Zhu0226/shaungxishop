package com.seckill.presentation.exception;

import com.seckill.common.ApiResponse;
import com.seckill.common.ErrorCode;
import com.seckill.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest req) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        log.warn("Validation failed: {} {}", req.getRequestURI(), msg);
        return ApiResponse.error(ErrorCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingHeader(MissingRequestHeaderException e, HttpServletRequest req) {
        String msg = "缺少必要请求头: " + e.getHeaderName();
        log.warn("Missing header: {} {}", req.getRequestURI(), msg);
        return ApiResponse.error(ErrorCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest req) {
        int code = e.getCode();
        HttpStatus status = code == 404 ? HttpStatus.NOT_FOUND
                : code == 429 ? HttpStatus.TOO_MANY_REQUESTS
                : code >= 400 && code < 500 ? HttpStatus.BAD_REQUEST
                : HttpStatus.INTERNAL_SERVER_ERROR;
        log.debug("Business exception: {} code={} msg={}", req.getRequestURI(), code, e.getMessage());
        return ResponseEntity.status(status).body(ApiResponse.error(code, e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("Illegal argument: {} {}", req.getRequestURI(), e.getMessage());
        return ApiResponse.error(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest req) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        if (msg.isEmpty()) msg = "参数校验失败";
        log.warn("Constraint violation: {} {}", req.getRequestURI(), msg);
        return ApiResponse.error(ErrorCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleRedisConnectionFailure(RedisConnectionFailureException e, HttpServletRequest req) {
        log.warn("Redis connection failure: {} {}", req.getRequestURI(), e.getMessage());
        return ApiResponse.error(503, "缓存服务暂时不可用，请检查 Redis 是否启动或稍后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGenericException(Exception e, HttpServletRequest req) {
        String exType = e.getClass().getName();
        log.error("Unhandled exception type={} uri={} msg={}", exType, req.getRequestURI(), e.getMessage(), e);
        String friendlyMsg = resolveFriendlyMessage(e);
        if (friendlyMsg != null) {
            return ApiResponse.error(503, friendlyMsg);
        }
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
    }

    private String resolveFriendlyMessage(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String name = t.getClass().getName();
            String msg = t.getMessage();
            if (msg == null) {
                t = t.getCause();
                continue;
            }
            String lower = msg.toLowerCase();
            if (name.contains("Redis") || name.contains("redis")
                    || lower.contains("connection refused")
                    || lower.contains("unable to connect")
                    || lower.contains("connection reset")
                    || lower.contains("redis")
                    || lower.contains("could not connect")) {
                return "缓存服务暂时不可用，请检查 Redis 是否已启动（默认 localhost:6379）";
            }
            if (lower.contains("communications link failure") || lower.contains("connection refused")) {
                return "数据库连接失败，请检查 MySQL 是否已启动及配置是否正确";
            }
            t = t.getCause();
        }
        return null;
    }
}
