package top.sincs.sinbot.exception;

import top.sincs.sinbot.constant.ErrorCode;

/**
 * 业务异常。
 *
 * <p>业务分支中需要中断流程时抛出，由 {@link GlobalExceptionHandler} 转成统一响应体。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * @param errorCode 错误码，决定响应中的 {@code code}
     * @param message   更具体的错误描述，覆盖 {@code ErrorCode} 中的默认文案
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
