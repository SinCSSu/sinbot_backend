package top.sincs.sinbot.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.exception.BusinessException;

/**
 * 当前登录用户的读取入口。
 *
 * <p>业务代码一律通过这里获取用户，不要直接依赖 {@code SecurityContextHolder}。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户，未登录时抛未认证异常 */
    public static LoginUser getLoginUser() {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    /** 获取当前登录用户，未登录返回 {@code null}（用于审计字段填充等可空场景） */
    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }

    /** 获取当前登录用户 ID */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }
}
