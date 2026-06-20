package com.puxun.monitor.datasource;

import com.puxun.monitor.common.BizException;
import com.puxun.monitor.datasource.security.SqlReadOnlyValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlReadOnlyValidatorTest {

    @Test
    void allows_select() {
        assertDoesNotThrow(() -> SqlReadOnlyValidator.assertSelectOnly(
                "SELECT SUM(amount) FROM ar_detail WHERE period = '2026-06'"));
        assertDoesNotThrow(() -> SqlReadOnlyValidator.assertSelectOnly(
                "WITH t AS (SELECT 1 a) SELECT a FROM t"));
    }

    @Test
    void rejects_writes_and_ddl() {
        assertThrows(BizException.class, () -> SqlReadOnlyValidator.assertSelectOnly(
                "UPDATE gl_voucher_entry SET debit = 0"));
        assertThrows(BizException.class, () -> SqlReadOnlyValidator.assertSelectOnly(
                "DELETE FROM ar_detail"));
        assertThrows(BizException.class, () -> SqlReadOnlyValidator.assertSelectOnly(
                "DROP TABLE ar_detail"));
        assertThrows(BizException.class, () -> SqlReadOnlyValidator.assertSelectOnly(
                "INSERT INTO ar_detail(amount) VALUES (1)"));
    }

    @Test
    void rejects_multi_statement() {
        assertThrows(BizException.class, () -> SqlReadOnlyValidator.assertSelectOnly(
                "SELECT 1; DELETE FROM ar_detail"));
    }

    @Test
    void rejects_blank() {
        assertThrows(BizException.class, () -> SqlReadOnlyValidator.assertSelectOnly("  "));
    }
}
