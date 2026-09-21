package top.sincs.sinbot.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

public class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "creationDate", LocalDateTime.class, now);
        strictInsertFill(metaObject, "lastUpdateDate", LocalDateTime.class, now);
        // createBy / lastUpdateBy 需要从当前登录用户获取，接入 security 后补充
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "lastUpdateDate", LocalDateTime.class, LocalDateTime.now());
    }
}
