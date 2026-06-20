package com.puxun.monitor.datasource.runtime;

import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.datasource.domain.DataSourceEntity;
import com.puxun.monitor.datasource.mapper.DataSourceMapper;
import com.puxun.monitor.datasource.security.CipherService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务库动态数据源管理：按 dataSourceId 懒建并缓存只读连接池，按客户隔离限流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessDataSourceManager {

    private final DataSourceMapper dataSourceMapper;
    private final CipherService cipherService;

    private final Map<Long, HikariDataSource> pools = new ConcurrentHashMap<>();

    /** 每客户连接池上限，防止巡检冲击业务主库。 */
    private static final int PER_DS_MAX_POOL = 5;

    public DataSource resolve(Long dataSourceId) {
        return pools.computeIfAbsent(dataSourceId, this::build);
    }

    public DataSourceEntity meta(Long dataSourceId) {
        DataSourceEntity e = dataSourceMapper.selectById(dataSourceId);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "数据源不存在: " + dataSourceId);
        }
        return e;
    }

    private HikariDataSource build(Long id) {
        DataSourceEntity e = meta(id);
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(e.getJdbcUrl());
        cfg.setUsername(e.getUsername());
        cfg.setPassword(cipherService.decrypt(e.getPasswordCipher()));
        cfg.setDriverClassName(driverOf(e.getDbType()));
        cfg.setReadOnly(true);                 // 连接级只读
        cfg.setMaximumPoolSize(PER_DS_MAX_POOL);
        cfg.setMinimumIdle(0);
        cfg.setConnectionTimeout(5000);
        cfg.setValidationTimeout(3000);
        cfg.setMaxLifetime(30 * 60 * 1000L);
        cfg.setPoolName("biz-ds-" + id);
        String initSql = readOnlyInitSql(e.getDbType());
        if (initSql != null) {
            cfg.setConnectionInitSql(initSql);
        }
        log.info("构建业务库连接池 dataSourceId={}, type={}, mode={}", id, e.getDbType(), e.getMode());
        return new HikariDataSource(cfg);
    }

    public void evict(Long dataSourceId) {
        HikariDataSource ds = pools.remove(dataSourceId);
        if (ds != null) {
            ds.close();
        }
    }

    /** 探活：在该数据源上执行心跳，返回耗时(ms)，失败抛异常。 */
    public long probe(Long dataSourceId) {
        DataSourceEntity e = meta(dataSourceId);
        long t0 = System.currentTimeMillis();
        try (var conn = resolve(dataSourceId).getConnection();
             var st = conn.createStatement()) {
            st.execute(heartbeat(e.getDbType()));
            return System.currentTimeMillis() - t0;
        } catch (Exception ex) {
            // 失败时失效连接池，下次重建
            evict(dataSourceId);
            throw new BizException(ResultCode.BIZ_ERROR, "数据源连接失败: " + ex.getMessage());
        }
    }

    public void updateProbeResult(Long id, String status, Integer latencyMs) {
        DataSourceEntity upd = new DataSourceEntity();
        upd.setId(id);
        upd.setStatus(status);
        upd.setLatencyMs(latencyMs);
        upd.setLastProbeAt(Instant.now());
        dataSourceMapper.updateById(upd);
    }

    public void closeAll() {
        pools.values().forEach(HikariDataSource::close);
        pools.clear();
    }

    /** 用一组连接参数做一次性连通性测试（不保存、不缓存），返回耗时(ms)。 */
    public long testTransient(String dbType, String jdbcUrl, String username, String rawPassword) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(username);
        cfg.setPassword(rawPassword);
        cfg.setDriverClassName(driverOf(dbType));
        cfg.setReadOnly(true);
        cfg.setMaximumPoolSize(1);
        cfg.setConnectionTimeout(5000);
        cfg.setPoolName("biz-ds-test");
        long t0 = System.currentTimeMillis();
        try (HikariDataSource ds = new HikariDataSource(cfg);
             var conn = ds.getConnection();
             var st = conn.createStatement()) {
            st.execute(heartbeat(dbType));
            return System.currentTimeMillis() - t0;
        } catch (Exception ex) {
            throw new BizException(ResultCode.BIZ_ERROR, "连接测试失败: " + ex.getMessage());
        }
    }

    private String driverOf(String dbType) {
        Enums.DbType t = Enums.DbType.valueOf(dbType);
        return switch (t) {
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case POSTGRES -> "org.postgresql.Driver";
            case ORACLE -> "oracle.jdbc.OracleDriver";
            case SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case DM -> "dm.jdbc.driver.DmDriver";
        };
    }

    private String heartbeat(String dbType) {
        Enums.DbType t = Enums.DbType.valueOf(dbType);
        return switch (t) {
            case ORACLE, DM -> "SELECT 1 FROM DUAL";
            default -> "SELECT 1";
        };
    }

    private String readOnlyInitSql(String dbType) {
        Enums.DbType t = Enums.DbType.valueOf(dbType);
        return switch (t) {
            case MYSQL -> "SET SESSION TRANSACTION READ ONLY";
            case POSTGRES -> "SET default_transaction_read_only = on";
            default -> null;   // Oracle/SQLServer/DM 依赖连接级 readOnly + 只读账号
        };
    }
}
