package com.cyberflow.admin.dashboard.service;

import com.cyberflow.admin.common.DataScope;
import com.cyberflow.admin.common.DataScopeService;
import com.cyberflow.admin.dashboard.mapper.EcommerceProductMapper;
import com.cyberflow.admin.dashboard.mapper.OrderMapper;
import com.cyberflow.admin.dashboard.mapper.SiteIndexingHistoryMapper;
import com.cyberflow.admin.dashboard.mapper.SiteInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderSharedFieldScopeTest {
    @Test
    @SuppressWarnings("unchecked")
    void sharedRowsAndSummaryAreTrimmedByTheBackendWhitelist() {
        OrderMapper orders = mock(OrderMapper.class);
        DataScopeService scopes = mock(DataScopeService.class);
        when(scopes.current()).thenReturn(new DataScope(false, false, "A-本人", List.of("A-成员"),
                Set.of("order.amount", "order.currency")));
        when(orders.countOrdersFiltered(any(), any(), any(), any(), any(), any(), any(),
                eq("A-本人,A-成员"), any(), any())).thenReturn(1L);
        Map<String, Object> sharedRow = new LinkedHashMap<>();
        sharedRow.put("id", 99L);
        sharedRow.put("amount", 12.34);
        sharedRow.put("currency", "USD");
        sharedRow.put("shipping_email", "secret@example.com");
        sharedRow.put("admin_name", "A-成员");
        when(orders.listOrdersFiltered(any(), any(), any(), any(), any(), any(), any(),
                eq("A-本人,A-成员"), any(), any(), anyInt(), anyInt())).thenReturn(List.of(sharedRow));
        when(orders.summarizeOrdersFiltered(any(), any(), any(), any(), any(), any(), any(),
                eq("A-本人,A-成员"), any(), any())).thenReturn(new LinkedHashMap<>(Map.of(
                        "total_count", 1L, "total_amount", 12.34,
                        "paid_count", 1L, "paid_amount", 12.34)));
        DashboardService service = new DashboardService(
                mock(SiteInfoMapper.class), orders, mock(SiteIndexingHistoryMapper.class),
                mock(EcommerceProductMapper.class), mock(StringRedisTemplate.class), scopes);

        Map<String, Object> result = service.getOrders(
                1, 20, null, null, null, null, null, null, null, null, null);

        Map<?, ?> row = ((List<Map<String, Object>>) result.get("list")).get(0);
        assertEquals(Set.of("amount", "currency"), row.keySet());
        Map<?, ?> summary = (Map<?, ?>) result.get("summary");
        assertEquals(12.34, summary.get("total_amount"));
        assertFalse(summary.containsKey("paid_count"));
        assertFalse(summary.containsKey("paid_amount"));
    }
}
