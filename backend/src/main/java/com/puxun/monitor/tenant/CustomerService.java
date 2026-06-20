package com.puxun.monitor.tenant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.datasource.domain.DataSourceEntity;
import com.puxun.monitor.datasource.mapper.DataSourceMapper;
import com.puxun.monitor.datasource.runtime.DataSourceFacade;
import com.puxun.monitor.tenant.domain.*;
import com.puxun.monitor.tenant.dto.CustomerDtos.*;
import com.puxun.monitor.tenant.mapper.CustomerMapper;
import com.puxun.monitor.tenant.mapper.TenantMapper;
import com.puxun.monitor.tenant.mapper.CustomerLedgerMapper;
import com.puxun.monitor.tenant.mapper.CustomerCurrencyMapper;
import com.puxun.monitor.tenant.mapper.CustomerConsolidationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final TenantMapper tenantMapper;
    private final CustomerLedgerMapper ledgerMapper;
    private final CustomerCurrencyMapper currencyMapper;
    private final CustomerConsolidationMapper consolidationMapper;
    private final DataSourceMapper dataSourceMapper;
    private final DataSourceFacade dataSourceFacade;

    public IPage<Customer> page(String industry, String isolationMode, String keyword, long page, long size) {
        return customerMapper.selectPage(new Page<>(page, size),
                Wrappers.<Customer>lambdaQuery()
                        .eq(industry != null && !industry.isBlank(), Customer::getIndustry, industry)
                        .eq(isolationMode != null && !isolationMode.isBlank(), Customer::getIsolationMode, isolationMode)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Customer::getName, keyword).or().like(Customer::getCode, keyword))
                        .orderByDesc(Customer::getCreatedAt));
    }

    @Audited(action = "EDIT_CUSTOMER")
    @Transactional
    public Long save(SaveCmd cmd) {
        Customer c = (cmd.id() != null) ? customerMapper.selectById(cmd.id()) : new Customer();
        if (c == null) throw new BizException(ResultCode.NOT_FOUND, "客户不存在");
        c.setCode(cmd.code());
        c.setName(cmd.name());
        c.setIndustry(cmd.industry());
        c.setIsolationMode(cmd.isolationMode());
        c.setProductLine(cmd.productLine());
        c.setDeployVersion(cmd.deployVersion());
        c.setServerGroup(cmd.serverGroup());
        c.setAccountingPeriodType(cmd.accountingPeriodType());
        c.setCurrentPeriod(cmd.currentPeriod());
        c.setClosingRule(cmd.closingRule());
        c.setSlaLevel(cmd.slaLevel());
        c.setSlaAvailability(cmd.slaAvailability());
        c.setSlaCheckFreq(cmd.slaCheckFreq());
        c.setSlaAlertSla(cmd.slaAlertSla());
        c.setWecomGroupRef(cmd.wecomGroupRef());
        c.setStatus(cmd.status() != null ? cmd.status() : "ACTIVE");
        if (cmd.id() != null) customerMapper.updateById(c); else customerMapper.insert(c);

        Long cid = c.getId();
        replaceChildren(cid, cmd);
        return cid;
    }

    private void replaceChildren(Long cid, SaveCmd cmd) {
        // 账簿
        ledgerMapper.delete(Wrappers.<CustomerLedger>lambdaQuery().eq(CustomerLedger::getCustomerId, cid));
        if (cmd.ledgers() != null) {
            for (LedgerItem it : cmd.ledgers()) {
                CustomerLedger l = new CustomerLedger();
                l.setCustomerId(cid); l.setLedgerName(it.ledgerName());
                l.setStandard(it.standard()); l.setMonitorEnabled(it.monitorEnabled() != null ? it.monitorEnabled() : 1);
                ledgerMapper.insert(l);
            }
        }
        // 币种
        currencyMapper.delete(Wrappers.<CustomerCurrency>lambdaQuery().eq(CustomerCurrency::getCustomerId, cid));
        if (cmd.currencies() != null) {
            for (CurrencyItem it : cmd.currencies()) {
                CustomerCurrency cur = new CustomerCurrency();
                cur.setCustomerId(cid); cur.setCurrencyCode(it.currencyCode());
                cur.setIsBase(it.isBase() != null ? it.isBase() : 0); cur.setFxSource(it.fxSource());
                currencyMapper.insert(cur);
            }
        }
        // 合并口径(单条)
        consolidationMapper.delete(Wrappers.<CustomerConsolidation>lambdaQuery().eq(CustomerConsolidation::getCustomerId, cid));
        if (cmd.consolidation() != null) {
            ConsolidationItem it = cmd.consolidation();
            CustomerConsolidation co = new CustomerConsolidation();
            co.setCustomerId(cid);
            co.setInConsolidation(it.inConsolidation() != null ? it.inConsolidation() : 0);
            co.setConsolidationLevel(it.consolidationLevel());
            co.setConsolidationEntity(it.consolidationEntity());
            co.setEliminationRuleSetId(it.eliminationRuleSetId());
            consolidationMapper.insert(co);
        }
    }

    public CustomerDetailVO detail(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BizException(ResultCode.NOT_FOUND, "客户不存在");
        List<CustomerLedger> ledgers = ledgerMapper.selectList(
                Wrappers.<CustomerLedger>lambdaQuery().eq(CustomerLedger::getCustomerId, id));
        List<CustomerCurrency> currencies = currencyMapper.selectList(
                Wrappers.<CustomerCurrency>lambdaQuery().eq(CustomerCurrency::getCustomerId, id));
        CustomerConsolidation consolidation = consolidationMapper.selectOne(
                Wrappers.<CustomerConsolidation>lambdaQuery().eq(CustomerConsolidation::getCustomerId, id), false);
        long tenantCount = tenantMapper.selectCount(
                Wrappers.<Tenant>lambdaQuery().eq(Tenant::getCustomerId, id));
        long dsCount = dataSourceMapper.selectCount(
                Wrappers.<DataSourceEntity>lambdaQuery().eq(DataSourceEntity::getCustomerId, id));
        return new CustomerDetailVO(c, ledgers, currencies, consolidation, tenantCount, dsCount);
    }

    public List<Tenant> tenants(Long customerId) {
        return tenantMapper.selectList(Wrappers.<Tenant>lambdaQuery().eq(Tenant::getCustomerId, customerId));
    }

    /**
     * 自动发现租户：在客户的业务库执行只读查询，取首列作为 tenant_key，新增不存在的租户。
     * 仅 TENANT_SHARED 模式可用。
     */
    @Audited(action = "DISCOVER_TENANT")
    @Transactional
    public int discoverTenants(Long customerId, DiscoverCmd cmd) {
        Customer c = customerMapper.selectById(customerId);
        if (c == null) throw new BizException(ResultCode.NOT_FOUND, "客户不存在");
        if (!Enums.IsolationMode.TENANT_SHARED.name().equals(c.getIsolationMode())) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅租户隔离模式支持自动发现租户");
        }
        Long dsId = cmd.dataSourceId();
        if (dsId == null) {
            DataSourceEntity ds = dataSourceMapper.selectOne(
                    Wrappers.<DataSourceEntity>lambdaQuery().eq(DataSourceEntity::getCustomerId, customerId).last("LIMIT 1"));
            if (ds == null) throw new BizException(ResultCode.BIZ_ERROR, "客户尚未配置数据源");
            dsId = ds.getId();
        }
        List<Map<String, Object>> rows = dataSourceFacade.rows(dsId, cmd.querySql(), Map.of(), 100000);
        int added = 0;
        for (Map<String, Object> row : rows) {
            Object v = row.values().stream().findFirst().orElse(null);
            if (v == null) continue;
            String key = String.valueOf(v);
            long exists = tenantMapper.selectCount(Wrappers.<Tenant>lambdaQuery()
                    .eq(Tenant::getCustomerId, customerId).eq(Tenant::getTenantKey, key));
            if (exists == 0) {
                Tenant t = new Tenant();
                t.setCustomerId(customerId);
                t.setTenantKey(key);
                t.setName(key);
                t.setCurrentPeriod(c.getCurrentPeriod());
                t.setStatus("ACTIVE");
                tenantMapper.insert(t);
                added++;
            }
        }
        return added;
    }
}
