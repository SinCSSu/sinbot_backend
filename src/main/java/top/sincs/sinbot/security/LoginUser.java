package top.sincs.sinbot.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 登录态载体，以 JSON 形式存入 Redis。
 *
 * <p>字段保持精简且不含敏感信息：Redis 被读取时不应泄漏任何凭证，
 * 真正的密码摘要只存在于 {@code sys_user} 表中。</p>
 */
@Getter
@Setter
public class LoginUser implements Serializable {

    private Long userId;

    private String username;

    private String nickname;

    /** 形如 ROLE_ADMIN，直接作为 Spring Security 的 authority */
    private List<String> roles;

    /** 本次登录时间戳，用于计算绝对过期上限 */
    private long loginTime;

    /** 当前请求携带的 token，仅运行时使用，不写入 Redis */
    @JsonIgnore
    private String token;
}
