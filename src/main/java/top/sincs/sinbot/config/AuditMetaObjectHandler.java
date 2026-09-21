package top.sincs.sinbot.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "creationDate", LocalDateTime.class, now);
        strictInsertFill(metaObject, "lastUpdateDate", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", String.class, "-1");
        strictInsertFill(metaObject, "lastUpdateBy", String.class, "-1");
        strictInsertFill(metaObject, "deleteFlag", String.class, "N");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "lastUpdateDate", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "lastUpdateBy", String.class, "-1");
    }
}
