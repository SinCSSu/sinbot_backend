package top.sincs.sinbot.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 上游（第三方）响应外壳的映射模型。
 *
 * <p>刻意独立于此项目对外使用的 {@code top.sincs.sinbot.common.Result}：
 * 第三方字段命名可能与本系统不一致，若直接复用对外壳，为满足对方结构而做的兼容会污染已对外承诺的契约。</p>
 *
 * @param <T> data 的类型
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteResult<T> {

    private static final int SUCCESS_CODE = 0;

    /**
     * 业务码；为 null 时按成功处理（部分上游成功时不返回该字段）。
     */
    @JsonAlias({"errcode", "status", "ret"})
    private Integer code;

    @JsonAlias({"msg", "errmsg"})
    private String message;

    @JsonAlias({"result", "payload"})
    private T data;

    public boolean isSuccess() {
        return code == null || code == SUCCESS_CODE;
    }
}
