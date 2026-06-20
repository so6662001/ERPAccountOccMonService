package com.puxun.monitor.security.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;     // BCrypt
    private String displayName;
    private String mobile;
    private String wecomUserId;  // 企业微信成员 ID
    private String status;       // ACTIVE | DISABLED
}
