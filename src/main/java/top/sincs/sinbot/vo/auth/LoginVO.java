package top.sincs.sinbot.vo.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** 登录出参。 */
@Getter
@Setter
@Accessors(chain = true)
public class LoginVO {

    /** 调用方需以 {@code Authorization: Bearer {token}} 回传 */
    private String token;

    /** 固定为 Bearer */
    private String tokenType = "Bearer";

    /** 滑动过期窗口（秒），仅供客户端做续期判断，服务端会按实际访问续期 */
    private long expiresIn;

    private Long userId;

    private String username;

    private String nickname;

    private String role;
}
