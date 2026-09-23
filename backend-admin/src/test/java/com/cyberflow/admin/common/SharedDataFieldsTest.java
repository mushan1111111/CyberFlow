package com.cyberflow.admin.common;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedDataFieldsTest {
    @Test
    void sharedOwnersAreOnlyIncludedForModulesWithGrantedFields() {
        var scope = new DataScope(false, false, "A-本人", List.of("A-成员"),
                Set.of("order.amount"));

        assertEquals("A-本人", scope.ownerFilterFor("performance."));
        assertEquals("A-本人,A-成员", scope.ownerFilterFor("order."));
    }

    @Test
    void orderRowsKeepOnlyExplicitlyGrantedFields() {
        var scope = new DataScope(false, false, "A-本人", List.of("A-成员"),
                Set.of("order.amount", "order.currency"));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("amount", 12.34);
        row.put("currency", "USD");
        row.put("shipping_email", "secret@example.com");

        SharedDataFields.retainOrderFields(row, scope);

        assertEquals(Set.of("amount", "currency"), row.keySet());
        assertFalse(row.containsKey("id"));
    }

    @Test
    void performanceRowsKeepOnlyExplicitlyGrantedFields() {
        var scope = new DataScope(false, false, "A-本人", List.of("A-成员"),
                Set.of("performance.site_count"));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("real_name", "A-成员");
        row.put("site_count", 20L);
        row.put("successful_amount", 99.0);

        SharedDataFields.retainPerformanceFields(row, scope);

        assertEquals(Map.of("site_count", 20L), row);
        assertTrue(SharedDataFields.all().contains("order.shipping_address"));
    }
}
