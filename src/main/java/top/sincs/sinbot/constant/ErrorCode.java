package top.sincs.sinbot.constant;

import lombok.Getter;

/**
 * 统一错误码枚举。
 *
 * <p>分段约定：</p>
 * <ul>
 *     <li>0         —— 成功</li>
 *     <li>10xxx     —— 通用客户端错误（参数、认证、权限、资源不存在等）</li>
 *     <li>20xxx     —— 业务错误</li>
 *     <li>50xxx     —— 系统内部错误</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    /* ---------------- 成功 ---------------- */
    SUCCESS(0, "操作成功"),

    /* ---------------- 通用客户端错误 10xxx ---------------- */
    PARAM_ERROR(10000, "参数错误"),
    UNAUTHORIZED(10001, "未认证，请先登录"),
    FORBIDDEN(10002, "无权限访问该资源"),
    RESOURCE_NOT_FOUND(10003, "资源不存在"),
    METHOD_NOT_ALLOWED(10004, "请求方式不支持"),
    UNSUPPORTED_MEDIA_TYPE(10005, "媒体类型不支持"),
    LOGIN_FAILED(10006, "账号或密码错误"),
    ACCOUNT_DISABLED(10007, "账号已停用，请联系管理员"),
    TOKEN_INVALID(10008, "登录状态已失效，请重新登录"),
    ROBOT_API_KEY_INVALID(10009, "机器人凭据无效"),

    /* ---------------- 业务错误 20xxx ---------------- */
    BIZ_ERROR(20000, "业务处理失败"),
    DATA_NOT_FOUND(20001, "数据不存在"),
    DATA_ALREADY_EXISTS(20002, "数据已存在"),
    TEAM_INFO_NOT_EXISTS(20003,"团队不存在"),
    TEAM_ID_FORMAT_ERROR(20004,"团队ID格式错误"),
    TEAM_NOT_IN_GROUP(20005,"团队不属于当前群"),
    MEMBER_NOT_SIGNED_UP(20006,"未报名该团队"),
    MEMBER_RECORD_AMBIGUOUS(20007,"本团下你有多条报名记录，请指定游戏ID"),
    TEAM_NOT_RECRUITING(20008,"团队已开团或已结束，不可报名/退团"),
    TEAM_SETTLEMENT_WINDOW_CLOSED(20009,"团队结算窗口已关闭，需由管理端补录"),
    TEAM_SETTLEMENT_NOT_STARTED(20010,"团队尚未开团，暂不能登记结算信息"),
    SETTLEMENT_CONTENT_EMPTY(20011,"locker 与 salary 不允许同时为空"),

    /* ---------------- 系统错误 50xxx ---------------- */
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试"),
    UPSTREAM_ERROR(50001, "上游服务调用失败"),
    UPSTREAM_RESPONSE_INVALID(50002, "上游响应格式错误，无法解析"),
    ;

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
