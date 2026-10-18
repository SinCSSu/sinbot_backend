package top.sincs.sinbot.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
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

    /**
     * 取当前操作人的 QQ 号，缺失即为非法请求（DR-08）。
     *
     * <p>机器人来源必须显式带 {@code X-Operator-Qq}：{@code X-Api-Key} 只证明「请求来自机器人」，
     * 「谁在群里发指令」另有其人，且这个值没有任何旁证可推导。缺了它仍放行的话，
     * 落库的就是一条 created_by 为空的团队——「一个没人开的团」，追责链当场断掉。</p>
     *
     * <p>App 侧无需额外判别：username 来自服务端签发的 token，天然非空；若真为空，
     * 按同一口径拒绝也比静默写入 null 好。</p>
     *
     * @return 去空白后的操作人 QQ
     * @throws BusinessException 当前身份没有操作人标识（机器人未带 {@code X-Operator-Qq}）
     */
    public static String requireOperatorQq() {
        LoginUser loginUser = getLoginUser();
        if (!StringUtils.hasText(loginUser.getUsername())) {
            throw new BusinessException(ErrorCode.OPERATOR_QQ_MISSING);
        }
        return loginUser.getUsername().trim();
    }

    /** 获取当前登录用户 ID */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }
}
