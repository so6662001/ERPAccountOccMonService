package com.puxun.monitor.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.puxun.monitor.datasource.domain.DataSourceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSourceMapper extends BaseMapper<DataSourceEntity> {
}
