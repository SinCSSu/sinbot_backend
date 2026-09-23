package top.sincs.sinbot.constant;

/**
 * 自定义请求头常量。
 *
 * <p>机器人侧不参与业务流程参数的拼装，群号与操作人一律由请求头带入（DR-04 / DR-08）。</p>
 */
public final class HeaderConstant {

    private HeaderConstant() {
    }

    /** 机器人所在群的群号，业务分区键（DR-04） */
    public static final String GROUP_NUMBER = "X-Group-Number";

    /** 群内实际发指令的人的 QQ 号，服务端据此填充 created_by / member_qq_number（DR-08） */
    public static final String OPERATOR_QQ = "X-Operator-Qq";

    /** 机器人实例的服务间凭据（DR-03，认证通道尚未落地，见 P-06） */
    public static final String API_KEY = "X-Api-Key";
}
