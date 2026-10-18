package top.sincs.sinbot.constant;

/**
 * 鉴权通道之间共享的字符串常量。
 *
 * <p>机器人通道（{@code X-Api-Key}）与 App 通道（Bearer token）分属两个业务模块，
 * 但 401 出口 {@code RestAuthenticationEntryPoint} 位于公共层，需要同时读取两条通道
 * 写入的 request 属性名才能区分错误码。若这些常量留在各自的 Filter 上，
 * 公共层就必须反向依赖 bot / app 模块，形成依赖倒置——因此统一收敛到这里。</p>
 *
 * <p>三个常量的值必须与两侧 Filter 写入/读取的完全一致，改动时请一并核对。</p>
 */
public final class SecurityAttrConstant {

    private SecurityAttrConstant() {
    }

    /** 机器人携带了 {@code X-Api-Key} 但无效时写入的 request 属性名，供 401 出口报 10009 */
    public static final String ATTR_INVALID_ROBOT_KEY = "sinbot.invalid-robot-key";

    /** 携带了 Bearer token 但已失效 / 已登出时写入的 request 属性名，供 401 出口报 10008 */
    public static final String ATTR_INVALID_TOKEN = "sinbot.invalid-token";

    /** 机器人身份的角色标识 */
    public static final String ROLE_ROBOT = "ROLE_ROBOT";
}
