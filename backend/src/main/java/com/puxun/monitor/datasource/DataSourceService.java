package com.puxun.monitor.datasource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.datasource.domain.DataSourceEntity;
import com.puxun.monitor.datasource.dto.DataSourceDtos.*;
import com.puxun.monitor.datasource.mapper.DataSourceMapper;
import com.puxun.monitor.datasource.runtime.BusinessDataSourceManager;
import com.puxun.monitor.datasource.security.CipherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceMapper mapper;
    private final CipherService cipher;
    private final BusinessDataSourceManager manager;

    public List<DataSourceVO> listByCustomer(Long customerId) {
        return mapper.selectList(Wrappers.<DataSourceEntity>lambdaQuery()
                        .eq(customerId != null, DataSourceEntity::getCustomerId, customerId))
                .stream().map(this::toVO).toList();
    }

    @Audited(action = "EDIT_DATASOURCE")
    @Transactional
    public DataSourceVO save(SaveCmd cmd) {
        DataSourceEntity e = (cmd.id() != null) ? mapper.selectById(cmd.id()) : new DataSourceEntity();
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "数据源不存在");
        }
        e.setCustomerId(cmd.customerId());
        e.setName(cmd.name());
        e.setDbType(cmd.dbType());
        e.setJdbcUrl(cmd.jdbcUrl());
        e.setUsername(cmd.username());
        e.setMode(cmd.mode() != null ? cmd.mode() : "READ_REPLICA");
        e.setTenantColumn(cmd.tenantColumn() != null ? cmd.tenantColumn() : "tenant_id");
        // 口令：有传则加密更新；为空则保留原值
        if (cmd.password() != null && !cmd.password().isBlank()) {
            e.setPasswordCipher(cipher.encrypt(cmd.password()));
        }
        if (e.getStatus() == null) {
            e.setStatus("ONLINE");
        }
        if (cmd.id() != null) {
            mapper.updateById(e);
            manager.evict(e.getId());     // 配置变更后失效重建
        } else {
            mapper.insert(e);
        }
        return toVO(e);
    }

    @Audited(action = "EDIT_DATASOURCE", target = "delete")
    public void delete(Long id) {
        mapper.deleteById(id);
        manager.evict(id);
    }

    /** 对已保存数据源探活并更新状态。 */
    public TestResult probe(Long id) {
        try {
            long latency = manager.probe(id);
            String status = latency > 200 ? "DELAY" : "ONLINE";
            manager.updateProbeResult(id, status, (int) latency);
            return new TestResult(true, latency, status);
        } catch (BizException e) {
            manager.updateProbeResult(id, "FAILED", null);
            return new TestResult(false, -1, e.getMessage());
        }
    }

    public void probeAll() {
        mapper.selectList(Wrappers.<DataSourceEntity>lambdaQuery())
                .forEach(d -> probe(d.getId()));
    }

    /** 用一组参数做一次性连通性测试（不保存）。 */
    public TestResult test(TestCmd cmd) {
        try {
            long latency = manager.testTransient(cmd.dbType(), cmd.jdbcUrl(), cmd.username(), cmd.password());
            return new TestResult(true, latency, "连接成功");
        } catch (BizException e) {
            return new TestResult(false, -1, e.getMessage());
        }
    }

    private DataSourceVO toVO(DataSourceEntity e) {
        return new DataSourceVO(e.getId(), e.getCustomerId(), e.getName(), e.getDbType(), e.getJdbcUrl(),
                e.getUsername(), e.getMode(), e.getTenantColumn(), e.getStatus(), e.getLastProbeAt(), e.getLatencyMs());
    }
}
