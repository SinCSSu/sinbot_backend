package top.sincs.sinbot.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 对外接口的统一响应壳。
 *
 * <p>约定：所有接口无论成功与否，HTTP 状态码均为 200，
 * 具体业务结果通过 {@code code} 判断（{@code 0} 表示成功）。</p>
 *
 * @param <T> 响应数据类型
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    private int code;

    private String message;

    private T data;

    private long timestamp = System.currentTimeMillis();

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 用于携带更具体的错误信息，如参数校验失败的字段描述。
     */
    public static <T> Result<T> failure(ErrorCode errorCode, String detailMessage) {
        return new Result<>(errorCode.getCode(), detailMessage, null);
    }

    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 仅由 {@code GlobalResponseAdvice} 在加壳时判断是否已包装过。
     */
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }
}
