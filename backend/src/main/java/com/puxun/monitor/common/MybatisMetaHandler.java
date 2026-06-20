package com.puxun.monitor.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.puxun.monitor.security.support.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 自动填充 createdAt/updatedAt/createdBy/updatedBy/deleted。
 */
@Component
public class MybatisMetaHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Instant now = Instant.now();
        String user = SecurityUtils.currentUsernameOrSystem();
        this.strictInsertFill(metaObject, "createdAt", Instant.class, now);
        this.strictInsertFill(metaObject, "updatedAt", Instant.class, now);
        this.strictInsertFill(metaObject, "createdBy", String.class, user);
        this.strictInsertFill(metaObject, "updatedBy", String.class, user);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
        this.strictUpdateFill(metaObject, "updatedBy", String.class, SecurityUtils.currentUsernameOrSystem());
    }
}
