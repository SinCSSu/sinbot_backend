package top.sincs.sinbot.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.sincs.sinbot.security.LoginUser;
import top.sincs.sinbot.security.SecurityUtils;

import java.time.LocalDateTime;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /** 无身份上下文（启动任务、内部调用）时的兜底操作人 */
    private static final String ANONYMOUS_OPERATOR = "-1";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String operator = currentOperator();
        strictInsertFill(metaObject, "creationDate", LocalDateTime.class, now);
        strictInsertFill(metaObject, "lastUpdateDate", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", String.class, operator);
        strictInsertFill(metaObject, "lastUpdateBy", String.class, operator);
        strictInsertFill(metaObject, "deleteFlag", String.class, "N");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新必须覆盖旧值：实体的字段往往带着库里读出的历史值，
        // strictUpdateFill 遇非空即跳过，这里改用 setFieldValByName 强制写入
        setFieldValByName("lastUpdateDate", LocalDateTime.now(), metaObject);
        setFieldValByName("lastUpdateBy", currentOperator(), metaObject);
    }

    /**
     * 取当前操作人，统一填进 {@code created_by} / {@code last_update_by}（DR-06 / DR-08）。
     *
     * <p>机器人来源的操作人即 {@code X-Operator-Qq}，已由 {@code RobotAuthenticationFilter}
     * 写入身份上下文的 {@code username}；App 来源的 {@code username} 本身就是 QQ 号（DR-07）。
     * 两条链路口径一致，这里只读上下文，业务代码不再显式赋值。
     * 写入策略：{@code insertFill} 不覆盖业务已显式赋的值，{@code updateFill} 强制覆盖旧值。</p>
     *
     * <p>取不到身份时回落到 {@link #ANONYMOUS_OPERATOR}，避免无请求上下文的写入失败。</p>
     */
    private static String currentOperator() {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null || !StringUtils.hasText(loginUser.getUsername())) {
            return ANONYMOUS_OPERATOR;
        }
        return loginUser.getUsername().trim();
    }
}
