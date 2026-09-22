package com.cyberflow.admin.dashboard.service;

import com.cyberflow.admin.common.DataScopeService;
import com.cyberflow.admin.dashboard.mapper.EcommerceProductMapper;
import com.cyberflow.admin.dashboard.mapper.OrderMapper;
import com.cyberflow.admin.dashboard.mapper.SiteIndexingHistoryMapper;
import com.cyberflow.admin.dashboard.mapper.SiteInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteClearServiceTest {
    @Test
    void clearsIndexHistoryBeforeSitesAndReportsBothCounts() {
        SiteInfoMapper sites = mock(SiteInfoMapper.class);
        SiteIndexingHistoryMapper indexes = mock(SiteIndexingHistoryMapper.class);
        when(indexes.deleteAllIndexHistory()).thenReturn(27);
        when(sites.deleteAllSites()).thenReturn(8);
        DashboardService service = new DashboardService(
                sites,
                mock(OrderMapper.class),
                indexes,
                mock(EcommerceProductMapper.class),
                mock(StringRedisTemplate.class),
                mock(DataScopeService.class));

        Map<String, Object> result = service.clearAllSites();

        assertEquals(8, result.get("deleted_sites"));
        assertEquals(27, result.get("deleted_index_history"));
        var order = inOrder(indexes, sites);
        order.verify(indexes).deleteAllIndexHistory();
        order.verify(sites).deleteAllSites();
    }
}
