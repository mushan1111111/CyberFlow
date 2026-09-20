package com.cyberflow.admin.dashboard.service;

import com.cyberflow.admin.common.DataScopeService;
import com.cyberflow.admin.crawler.config.service.CrawlerConfigService;
import com.cyberflow.admin.dashboard.mapper.RevenueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Site level order ranking for a period with advanced filters.
 * <p>
 * A site row is produced by matching the order host with the registered site
 * domain, so the ranking shows exactly which sites produced orders. Rows are
 * enriched with the newest indexing snapshot (Google index count and product
 * count) so a site can be judged by traffic and catalogue size as well.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SiteOrderRankingService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    /** Whitelisted sort columns: the value is injected verbatim into ORDER BY. */
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "deduplicated_orders", "o.deduplicated_orders",
            "total_orders", "o.total_orders",
            "successful_orders", "o.successful_orders",
            "total_amount", "o.total_amount",
            "successful_amount", "o.successful_amount",
            "index_count", "index_count",
            "product_count", "product_count",
            "add_date", "add_date",
            "site_domain", "s.site_domain",
            "admin_name", "s.admin_name"
    );
    private static final String DEFAULT_SORT = "o.deduplicated_orders DESC, o.total_orders DESC, s.site_domain";

    private final RevenueMapper revenueMapper;
    private final CrawlerConfigService configService;
    private final DataScopeService dataScopeService;

    public Map<String, Object> search(int page, int size, Map<String, Object> filters) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(size, 1), 500);
        Map<String, Object> query = new LinkedHashMap<>(filters == null ? Map.of() : filters);

        var scope = dataScopeService.current();
        Map<String, Object> config = configService.getRevenueConfig();
        Map<String, List<String>> mergeMap = stringListMap(config.get("userMergeMap"));
        Map<String, String> teacherMap = stringMap(config.get("teacherMap"));
        if (scope.administrator()) {
            query.put("ownerName", null);
            query.put("teacherSuffixes", List.of());
        } else {
            query.put("ownerName", scope.ownerName());
            query.put("teacherSuffixes", teacherSuffixes(List.of(scope.ownerName()), teacherMap, mergeMap));
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (Objects.toString(query.get("startDate"), "").isBlank()) {
            query.put("startDate", today.withDayOfMonth(1).toString());
        }
        if (Objects.toString(query.get("endDate"), "").isBlank()) {
            query.put("endDate", today.toString());
        }

        List<Map<String, Object>> rows = revenueMapper.siteOrderRanking(query, orderBy(query));
        long totalOrders = 0;
        long deduplicatedOrders = 0;
        long successfulOrders = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal successfulAmount = BigDecimal.ZERO;
        List<Map<String, Object>> items = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            long rowTotal = number(row.get("total_orders")).longValue();
            long rowDedup = number(row.get("deduplicated_orders")).longValue();
            long rowSuccess = number(row.get("successful_orders")).longValue();
            BigDecimal rowTotalAmount = number(row.get("total_amount"));
            BigDecimal rowSuccessAmount = number(row.get("successful_amount"));
            totalOrders += rowTotal;
            deduplicatedOrders += rowDedup;
            successfulOrders += rowSuccess;
            totalAmount = totalAmount.add(rowTotalAmount);
            successfulAmount = successfulAmount.add(rowSuccessAmount);

            String adminName = Objects.toString(row.get("admin_name"), "");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("site_domain", Objects.toString(row.get("site_domain"), ""));
            item.put("admin_name", adminName);
            item.put("real_name", realName(adminName, mergeMap));
            item.put("user_group", Objects.toString(row.get("user_group"), ""));
            item.put("add_date", Objects.toString(row.get("add_date"), ""));
            item.put("theme_name", Objects.toString(row.get("theme_name"), ""));
            item.put("index_count", number(row.get("index_count")).longValue());
            item.put("product_count", number(row.get("product_count")).longValue());
            item.put("total_orders", rowTotal);
            item.put("deduplicated_orders", rowDedup);
            item.put("successful_orders", rowSuccess);
            item.put("total_amount", money(rowTotalAmount));
            item.put("successful_amount", money(rowSuccessAmount));
            items.add(item);
        }

        int fromIndex = Math.min((safePage - 1) * safeSize, Math.max(0, items.size()));
        int toIndex = Math.min(fromIndex + safeSize, items.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", items.size());
        result.put("list", items.subList(fromIndex, toIndex));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("site_count", items.size());
        summary.put("total_orders", totalOrders);
        summary.put("deduplicated_orders", deduplicatedOrders);
        summary.put("successful_orders", successfulOrders);
        summary.put("total_amount", money(totalAmount));
        summary.put("successful_amount", money(successfulAmount));
        result.put("summary", summary);
        return result;
    }

    /** Builds the ORDER BY expression from whitelisted column names only. */
    private static String orderBy(Map<String, Object> filters) {
        String column = SORT_COLUMNS.get(Objects.toString(filters.get("sortBy"), ""));
        if (column == null) {
            column = SORT_COLUMNS.get("deduplicated_orders");
        }
        boolean ascending = "asc".equalsIgnoreCase(Objects.toString(filters.get("sortDir"), ""));
        return column + (ascending ? " ASC" : " DESC") + ", s.site_domain";
    }

    private static List<String> teacherSuffixes(List<String> owners,
                                                Map<String, String> teacherMap,
                                                Map<String, List<String>> mergeMap) {
        return teacherMap.entrySet().stream()
                .filter(entry -> owners.contains(entry.getKey())
                        || owners.stream().anyMatch(owner -> realName(owner, mergeMap).equals(entry.getKey())))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static String realName(String adminName, Map<String, List<String>> mergeMap) {
        for (Map.Entry<String, List<String>> entry : mergeMap.entrySet()) {
            if (entry.getValue().contains(adminName)) return entry.getKey();
        }
        return adminName;
    }

    private static Map<String, String> stringMap(Object value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) map.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
        return result;
    }

    private static Map<String, List<String>> stringListMap(Object value) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (v instanceof Collection<?> collection) result.put(String.valueOf(k), collection.stream().map(String::valueOf).toList());
            });
        }
        return result;
    }

    private static BigDecimal number(Object value) {
        try { return new BigDecimal(Objects.toString(value, "0")); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
