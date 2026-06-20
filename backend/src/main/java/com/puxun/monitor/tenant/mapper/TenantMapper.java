package com.puxun.monitor.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puxun.monitor.tenant.domain.Tenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
