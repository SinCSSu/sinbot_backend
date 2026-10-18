package top.sincs.sinbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录账号，专用于承载认证信息，与业务表 {@code member} 解耦。
 *
 * <p>表名取 {@code sys_user} 而非 {@code user}：{@code user} 在 PostgreSQL 中是
 * 关键字/内置函数名，用它做表名会导致所有手写 SQL 都必须写成 {@code "user"}。</p>
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 账号启用 */
    public static final int STATUS_ENABLED = 1;

    /** 账号停用 */
    public static final int STATUS_DISABLED = 0;

    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long userId;

    private String username;

    /** BCrypt 摘要，绝不存明文 */
    private String password;

    private String nickname;

    /** 角色标识，如 ADMIN / USER，落库时不含 ROLE_ 前缀 */
    private String role;

    /** 1 启用，0 停用 */
    private Integer status;
}
