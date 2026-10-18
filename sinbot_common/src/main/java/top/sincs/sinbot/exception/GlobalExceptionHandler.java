package top.sincs.sinbot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.common.Result;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>异常不会经过 {@code ResponseBodyAdvice}，因此必须在这里转为 {@link Result}，
 * 且要求所有异常分支最终都以 HTTP 200 + 业务 code 的形式返回。</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "top.sincs.sinbot.controller")
public class GlobalExceptionHandler {

    /**
     * 业务异常，属可预期分支，仅记录 warn 日志。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * @Valid / @Validated 校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        List<ObjectError> errors = e.getBindingResult().getAllErrors();
        String detail = errors.stream()
                .map(error -> error instanceof org.springframework.validation.FieldError fieldError
                        ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.failure(ErrorCode.PARAM_ERROR, StringUtils.hasText(detail)
                ? detail
                : ErrorCode.PARAM_ERROR.getMessage());
    }

    /**
     * 请求体缺失或格式非法，无法反序列化。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.failure(ErrorCode.PARAM_ERROR, "请求体格式错误，无法解析");
    }

    /**
     * 请求方式不匹配（如用 GET 访问只支持 POST 的接口）。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方式不支持: {}", e.getMessage());
        return Result.failure(ErrorCode.METHOD_NOT_ALLOWED, e.getMessage());
    }

    /**
     * 兜底处理：返回 200 + 系统错误码，避免把堆栈细节暴露给调用方。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.failure(ErrorCode.SYSTEM_ERROR);
    }
}
