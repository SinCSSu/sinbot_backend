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
     * <p>机器人来源的头缺失已在 {@code RobotAuthenticationFilter} 前置拦截，
     * 这里的判空是同一口径的最后一道兜底，业务代码照常直接调用即可。</p>
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

    /**
     * 取当前请求所属群的群号，缺失即为非法请求（DR-04）。
     *
     * <p>群号是数据的分区键，不能由调用方在 body 里自称，一律取自机器人请求头
     * {@code X-Group-Number}，并在认证阶段写入身份上下文；读不到就拒绝，
     * 避免「查到别的群的数据」或「把团队建到空群号下」。</p>
     *
     * @return 去空白后的群号
     * @throws BusinessException 当前身份没有群号标识（机器人未带 {@code X-Group-Number}）
     */
    public static String requireGroupNumber() {
        LoginUser loginUser = getLoginUser();
        if (!StringUtils.hasText(loginUser.getGroupNumber())) {
            throw new BusinessException(ErrorCode.GROUP_NUMBER_MISSING);
        }
        return loginUser.getGroupNumber().trim();
    }

    /** 获取当前登录用户 ID */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }
}
