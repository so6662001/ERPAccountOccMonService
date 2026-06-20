package com.puxun.monitor.tenant.dto;

import com.puxun.monitor.tenant.domain.*;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class CustomerDtos {

    private CustomerDtos() {}

    public record LedgerItem(String ledgerName, String standard, Integer monitorEnabled) {}
    public record CurrencyItem(String currencyCode, Integer isBase, String fxSource) {}
    public record ConsolidationItem(Integer inConsolidation, String consolidationLevel,
                                    String consolidationEntity, Long eliminationRuleSetId) {}

    public record SaveCmd(
            Long id,
            @NotBlank String code,
            @NotBlank String name,
            String industry,
            @NotBlank String isolationMode,
            String productLine,
            String deployVersion,
            String serverGroup,
            String accountingPeriodType,
            String currentPeriod,
            String closingRule,
            String slaLevel,
            String slaAvailability,
            String slaCheckFreq,
            String slaAlertSla,
            String wecomGroupRef,
            String status,
            List<LedgerItem> ledgers,
            List<CurrencyItem> currencies,
            ConsolidationItem consolidation
    ) {}

    public record DiscoverCmd(@NotBlank String querySql, Long dataSourceId) {}

    public record CustomerDetailVO(
            Customer customer,
            List<CustomerLedger> ledgers,
            List<CustomerCurrency> currencies,
            CustomerConsolidation consolidation,
            long tenantCount,
            long dataSourceCount
    ) {}
}
