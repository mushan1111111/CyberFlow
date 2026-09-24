package com.cyberflow.admin.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** Raw aggregates used by the revenue and commission report. */
@Mapper
public interface RevenueMapper {

    /** Whole-group totals, including paid orders whose administrator is not yet assigned. */
    @Select({"<script>",
            "SELECT user_group, COUNT(DISTINCT " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + ") AS total_orders,",
            "COUNT(DISTINCT CASE WHEN is_valid = 0 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS valid_orders,",
            "COUNT(DISTINCT CASE WHEN pay_status_text = '已支付' THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS successful_orders,",
            "COUNT(DISTINCT CASE WHEN site_tag = 0 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS standalone_orders,",
            "COUNT(DISTINCT CASE WHEN site_tag = 1 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS batch_orders,",
            "COUNT(DISTINCT CASE WHEN site_tag = 2 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS copy_orders,",
            "COALESCE(SUM(CASE WHEN pay_status_text = '已支付' THEN amount ELSE 0 END), 0) AS original_amount",
            "FROM orders WHERE TRIM(COALESCE(user_group, '')) &lt;&gt; ''",
            "AND (#{userGroup} IS NULL OR user_group = #{userGroup})",
            "<if test='startDate != null and startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>",
            "<if test='endDate != null and endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
            "GROUP BY user_group", "</script>"})
    List<Map<String, Object>> groupOrderStats(@Param("userGroup") String userGroup,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);

    @Select({"<script>",
            "SELECT user_group, COALESCE(NULLIF(TRIM(customer_ip_country), ''), '未知') AS customer_country,",
            "COUNT(DISTINCT " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + ") AS order_count",
            "FROM orders WHERE TRIM(COALESCE(user_group, '')) &lt;&gt; ''",
            "AND (#{userGroup} IS NULL OR user_group = #{userGroup})",
            "<if test='startDate != null and startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>",
            "<if test='endDate != null and endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
            "GROUP BY user_group, customer_country", "</script>"})
    List<Map<String, Object>> groupOrderCountryStats(@Param("userGroup") String userGroup,
                                                      @Param("startDate") String startDate,
                                                      @Param("endDate") String endDate);

    @Select({"<script>",
            "SELECT admin_name, user_group, COUNT(DISTINCT " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + ") AS total_orders,",
            "COUNT(DISTINCT CASE WHEN is_valid = 0 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS valid_orders,",
            "COUNT(DISTINCT CASE WHEN pay_status_text = '已支付' THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS successful_orders,",
            "COUNT(DISTINCT CASE WHEN site_tag = 0 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS standalone_orders,",
            "COUNT(DISTINCT CASE WHEN site_tag = 1 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS batch_orders,",
            "COUNT(DISTINCT CASE WHEN site_tag = 2 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS copy_orders,",
            "COALESCE(SUM(CASE WHEN pay_status_text = '已支付' THEN amount ELSE 0 END), 0) AS original_amount,",
            "COALESCE(SUM(CASE WHEN pay_status_text = '已支付' AND site_tag = 1 THEN amount ELSE 0 END), 0) AS batch_site_amount",
            "FROM orders WHERE TRIM(COALESCE(admin_name, '')) &lt;&gt; ''",
            "AND TRIM(COALESCE(user_group, '')) &lt;&gt; '' AND (#{userGroup} IS NULL OR user_group = #{userGroup})",
            "AND (#{ownerName} IS NULL OR FIND_IN_SET(admin_name, #{ownerName}) &gt; 0 " +
            "<if test='teacherSuffixes != null and !teacherSuffixes.isEmpty()'> OR " +
            "<foreach collection='teacherSuffixes' item='suffix' separator=' OR '>admin_name LIKE CONCAT('%', #{suffix})</foreach>" +
            "</if>)",
            "<if test='startDate != null and startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>",
            "<if test='endDate != null and endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
            "GROUP BY admin_name, user_group", "</script>"})
    List<Map<String, Object>> adminOrderStats(@Param("userGroup") String userGroup,
                                               @Param("ownerName") String ownerName,
                                               @Param("teacherSuffixes") List<String> teacherSuffixes,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);

    @Select({"<script>",
            "SELECT admin_name, user_group, COALESCE(NULLIF(TRIM(customer_ip_country), ''), '未知') AS customer_country,",
            "COUNT(DISTINCT " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + ") AS order_count",
            "FROM orders WHERE TRIM(COALESCE(admin_name, '')) &lt;&gt; ''",
            "AND TRIM(COALESCE(user_group, '')) &lt;&gt; '' AND (#{userGroup} IS NULL OR user_group = #{userGroup})",
            "AND (#{ownerName} IS NULL OR FIND_IN_SET(admin_name, #{ownerName}) &gt; 0 " +
            "<if test='teacherSuffixes != null and !teacherSuffixes.isEmpty()'> OR " +
            "<foreach collection='teacherSuffixes' item='suffix' separator=' OR '>admin_name LIKE CONCAT('%', #{suffix})</foreach>" +
            "</if>)",
            "<if test='startDate != null and startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>",
            "<if test='endDate != null and endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
            "GROUP BY admin_name, user_group, customer_country", "</script>"})
    List<Map<String, Object>> adminOrderCountryStats(@Param("userGroup") String userGroup,
                                                      @Param("ownerName") String ownerName,
                                                      @Param("teacherSuffixes") List<String> teacherSuffixes,
                                                      @Param("startDate") String startDate,
                                                      @Param("endDate") String endDate);

    @Select({"<script>",
            "SELECT s.admin_name, s.user_group, COUNT(*) AS site_count, SUM(s.site_tag = 1) AS batch_site_count,",
            "COUNT(valid_sites.product_host) AS valid_ordered_site_count",
            "FROM site_info s LEFT JOIN (",
            "SELECT DISTINCT " + OrderMapper.NORMALIZED_PRODUCT_HOST_SQL + " AS product_host FROM orders",
            "WHERE is_valid = 0 AND TRIM(COALESCE(product_host, '')) &lt;&gt; ''",
            "<if test='startDate != null and startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>",
            "<if test='endDate != null and endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
            ") valid_sites ON LOWER(CASE WHEN LEFT(TRIM(s.site_domain), 4) = 'www.'",
            "THEN SUBSTRING(TRIM(s.site_domain), 5) ELSE TRIM(s.site_domain) END) = valid_sites.product_host",
            "WHERE TRIM(COALESCE(s.admin_name, '')) &lt;&gt; ''",
            "AND TRIM(COALESCE(s.user_group, '')) &lt;&gt; '' AND (#{userGroup} IS NULL OR s.user_group = #{userGroup})",
            "AND (#{ownerName} IS NULL OR FIND_IN_SET(s.admin_name, #{ownerName}) &gt; 0 " +
            "<if test='teacherSuffixes != null and !teacherSuffixes.isEmpty()'> OR " +
            "<foreach collection='teacherSuffixes' item='suffix' separator=' OR '>s.admin_name LIKE CONCAT('%', #{suffix})</foreach>" +
            "</if>)",
            "GROUP BY s.admin_name, s.user_group", "</script>"})
    List<Map<String, Object>> adminSiteStats(@Param("userGroup") String userGroup,
                                              @Param("ownerName") String ownerName,
                                              @Param("teacherSuffixes") List<String> teacherSuffixes,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    @Select({"<script>",
            "SELECT filtered_orders.admin_name, filtered_orders.user_group,",
            "COALESCE(NULLIF(TRIM(categories.category_name), ''), '未分类') AS category_name,",
            "COUNT(*) AS order_count",
            "FROM (SELECT DISTINCT admin_name, user_group,",
            OrderMapper.NORMALIZED_PRODUCT_HOST_SQL + " AS normalized_host,",
            OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " AS order_identity",
            "FROM orders WHERE TRIM(COALESCE(admin_name, '')) &lt;&gt; ''",
            "AND TRIM(COALESCE(user_group, '')) &lt;&gt; '' AND (#{userGroup} IS NULL OR user_group = #{userGroup})",
            "AND (#{ownerName} IS NULL OR FIND_IN_SET(admin_name, #{ownerName}) &gt; 0 " +
            "<if test='teacherSuffixes != null and !teacherSuffixes.isEmpty()'> OR " +
            "<foreach collection='teacherSuffixes' item='suffix' separator=' OR '>admin_name LIKE CONCAT('%', #{suffix})</foreach>" +
            "</if>)",
            "<if test='startDate != null and startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>",
            "<if test='endDate != null and endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
            ") filtered_orders LEFT JOIN site_info s ON",
            "LOWER(CASE WHEN LEFT(TRIM(s.site_domain), 4) = 'www.'",
            "THEN SUBSTRING(TRIM(s.site_domain), 5) ELSE TRIM(s.site_domain) END) = filtered_orders.normalized_host",
            "JOIN JSON_TABLE(CASE WHEN s.cat_names IS NULL OR JSON_LENGTH(s.cat_names) = 0",
            "THEN JSON_ARRAY(COALESCE(NULLIF(TRIM(s.product_category), ''), '未分类')) ELSE s.cat_names END,",
            "'$[*]' COLUMNS(category_name VARCHAR(100) PATH '$')) categories",
            "GROUP BY filtered_orders.admin_name, filtered_orders.user_group, category_name", "</script>"})
    List<Map<String, Object>> adminOrderCategoryStats(@Param("userGroup") String userGroup,
                                                       @Param("ownerName") String ownerName,
                                                       @Param("teacherSuffixes") List<String> teacherSuffixes,
                                                       @Param("startDate") String startDate,
                                                       @Param("endDate") String endDate);

    @Select({"<script>", "SELECT site_domain, admin_name, user_group, site_tag, cat_names, DATE_FORMAT(COALESCE(domain_applied_at, created_at), '%Y-%m') AS site_month",
            "FROM site_info WHERE TRIM(COALESCE(admin_name, '')) &lt;&gt; ''",
            "AND TRIM(COALESCE(user_group, '')) &lt;&gt; '' AND (#{userGroup} IS NULL OR user_group = #{userGroup})",
            "AND (#{ownerName} IS NULL OR FIND_IN_SET(admin_name, #{ownerName}) &gt; 0 " +
            "<if test='teacherSuffixes != null and !teacherSuffixes.isEmpty()'> OR " +
            "<foreach collection='teacherSuffixes' item='suffix' separator=' OR '>admin_name LIKE CONCAT('%', #{suffix})</foreach>" +
            "</if>)",
            "<if test='siteCreatedMonth != null and siteCreatedMonth != &quot;&quot;'> AND DATE_FORMAT(COALESCE(domain_applied_at, created_at), '%Y-%m') = #{siteCreatedMonth}</if>",
            "ORDER BY site_month DESC, user_group, admin_name", "</script>"})
    List<Map<String, Object>> revenueSites(@Param("userGroup") String userGroup,
                                           @Param("ownerName") String ownerName,
                                           @Param("teacherSuffixes") List<String> teacherSuffixes,
                                           @Param("siteCreatedMonth") String siteCreatedMonth);

    String SITE_RANKING_DOMAIN_SQL =
            "LOWER(CASE WHEN LEFT(TRIM(s.site_domain), 4) = 'www.' " +
            "THEN SUBSTRING(TRIM(s.site_domain), 5) ELSE TRIM(s.site_domain) END)";

    /**
     * Per-site order ranking: joins the site registry with order aggregates on
     * the normalized domain and the newest indexing snapshot. Only sites that
     * actually have orders in the period are returned.
     */
    String SITE_RANKING_SQL =
            "WITH ranked_index AS (SELECT LOWER(CASE WHEN LEFT(TRIM(h.site_domain), 4) = 'www.' " +
            "THEN SUBSTRING(TRIM(h.site_domain), 5) ELSE TRIM(h.site_domain) END) AS normalized_domain, " +
            "h.index_count, h.product_count, " +
            "ROW_NUMBER() OVER (PARTITION BY LOWER(CASE WHEN LEFT(TRIM(h.site_domain), 4) = 'www.' " +
            "THEN SUBSTRING(TRIM(h.site_domain), 5) ELSE TRIM(h.site_domain) END) " +
            "ORDER BY h.recorded_at DESC, h.id DESC) AS row_num FROM site_indexing_history h), " +
            "latest_index AS (SELECT normalized_domain, index_count, product_count FROM ranked_index WHERE row_num = 1) " +
            "SELECT s.site_domain, s.admin_name, s.user_group, s.theme_name, " +
            "DATE_FORMAT(COALESCE(s.domain_applied_at, s.created_at), '%Y-%m-%d') AS add_date, " +
            "COALESCE(l.index_count, 0) AS index_count, COALESCE(l.product_count, 0) AS product_count, " +
            "o.total_orders, o.deduplicated_orders, o.successful_orders, o.total_amount, o.successful_amount " +
            "FROM site_info s JOIN (" +
            "SELECT " + OrderMapper.NORMALIZED_PRODUCT_HOST_SQL + " AS normalized_host, " +
            "COUNT(*) AS total_orders, " +
            "COUNT(DISTINCT " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + ") AS deduplicated_orders, " +
            "COUNT(DISTINCT CASE WHEN pay_status_text = '已支付' THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS successful_orders, " +
            "COALESCE(SUM(amount), 0) AS total_amount, " +
            "COALESCE(SUM(CASE WHEN pay_status_text = '已支付' THEN amount ELSE 0 END), 0) AS successful_amount " +
            "FROM orders WHERE 1 = 1 " +
            "<if test='filters.startDate != null and filters.startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{filters.startDate}, ' 00:00:00')</if> " +
            "<if test='filters.endDate != null and filters.endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{filters.endDate}, INTERVAL 1 DAY)</if> " +
            "GROUP BY " + OrderMapper.NORMALIZED_PRODUCT_HOST_SQL + ") o ON o.normalized_host = " + SITE_RANKING_DOMAIN_SQL +
            " LEFT JOIN latest_index l ON l.normalized_domain = " + SITE_RANKING_DOMAIN_SQL +
            " WHERE TRIM(COALESCE(s.admin_name, '')) &lt;&gt; '' AND TRIM(COALESCE(s.user_group, '')) &lt;&gt; '' " +
            "<if test='filters.userGroup != null and filters.userGroup != &quot;&quot;'> AND s.user_group = #{filters.userGroup}</if> " +
            "<if test='filters.domain != null and filters.domain != &quot;&quot;'> AND s.site_domain LIKE CONCAT('%', #{filters.domain}, '%')</if> " +
            "<if test='filters.adminName != null and filters.adminName != &quot;&quot;'> AND s.admin_name LIKE CONCAT('%', #{filters.adminName}, '%')</if> " +
            "<if test='filters.themeName != null and filters.themeName != &quot;&quot;'> AND s.theme_name LIKE CONCAT('%', #{filters.themeName}, '%')</if> " +
            "<if test='filters.minOrders != null'> AND o.deduplicated_orders &gt;= #{filters.minOrders}</if> " +
            "<if test='filters.minSuccessfulOrders != null'> AND o.successful_orders &gt;= #{filters.minSuccessfulOrders}</if> " +
            "<if test='filters.minTotalAmount != null'> AND o.total_amount &gt;= #{filters.minTotalAmount}</if> " +
            "<if test='filters.minSuccessfulAmount != null'> AND o.successful_amount &gt;= #{filters.minSuccessfulAmount}</if> " +
            "<if test='filters.minIndexCount != null'> AND COALESCE(l.index_count, 0) &gt;= #{filters.minIndexCount}</if> " +
            "<if test='filters.minProductCount != null'> AND COALESCE(l.product_count, 0) &gt;= #{filters.minProductCount}</if> " +
            "<if test='filters.ownerName != null'> AND (FIND_IN_SET(s.admin_name, #{filters.ownerName}) &gt; 0 " +
            "<if test='filters.teacherSuffixes != null and !filters.teacherSuffixes.isEmpty()'> OR " +
            "<foreach collection='filters.teacherSuffixes' item='suffix' separator=' OR '>s.admin_name LIKE CONCAT('%', #{suffix})</foreach>" +
            "</if>)</if>";

    /**
     * Site order ranking for one period with advanced filters. The sort column
     * is a whitelisted expression built by the caller.
     */
    @Select({"<script>", SITE_RANKING_SQL, "ORDER BY ${orderBy}", "</script>"})
    List<Map<String, Object>> siteOrderRanking(@Param("filters") Map<String, Object> filters,
                                                @Param("orderBy") String orderBy);

    @Select({"<script>", "SELECT " + OrderMapper.NORMALIZED_PRODUCT_HOST_SQL + " AS product_host,",
            "COUNT(DISTINCT " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + ") AS total_orders,",
            "COUNT(DISTINCT CASE WHEN is_valid = 0 THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS valid_orders,",
            "COUNT(DISTINCT CASE WHEN pay_status_text = '已支付' THEN " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + " END) AS successful_orders,",
            "COALESCE(SUM(CASE WHEN pay_status_text = '已支付' THEN amount ELSE 0 END), 0) AS successful_amount",
            "FROM orders WHERE TRIM(COALESCE(product_host, '')) &lt;&gt; ''",
            "AND (#{ownerName} IS NULL OR FIND_IN_SET(admin_name, #{ownerName}) &gt; 0 " +
            "<if test='teacherSuffixes != null and !teacherSuffixes.isEmpty()'> OR " +
            "<foreach collection='teacherSuffixes' item='suffix' separator=' OR '>admin_name LIKE CONCAT('%', #{suffix})</foreach>" +
            "</if>)",
            "<if test='startDate != null and startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>",
            "<if test='endDate != null and endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
            "GROUP BY " + OrderMapper.NORMALIZED_PRODUCT_HOST_SQL, "</script>"})
    List<Map<String, Object>> revenueOrdersByDomain(@Param("startDate") String startDate,
                                                     @Param("endDate") String endDate,
                                                     @Param("ownerName") String ownerName,
                                                     @Param("teacherSuffixes") List<String> teacherSuffixes);

    @Select({"<script>", "SELECT " + OrderMapper.NORMALIZED_PRODUCT_HOST_SQL + " AS product_host,",
            "COALESCE(NULLIF(TRIM(customer_ip_country), ''), '未知') AS customer_country,",
            "COUNT(DISTINCT " + OrderMapper.DEDUPLICATED_ORDER_KEY_SQL + ") AS order_count",
            "FROM orders WHERE TRIM(COALESCE(product_host, '')) &lt;&gt; ''",
            "AND (#{ownerName} IS NULL OR FIND_IN_SET(admin_name, #{ownerName}) &gt; 0 " +
            "<if test='teacherSuffixes != null and !teacherSuffixes.isEmpty()'> OR " +
            "<foreach collection='teacherSuffixes' item='suffix' separator=' OR '>admin_name LIKE CONCAT('%', #{suffix})</foreach>" +
            "</if>)",
            "<if test='startDate != null and startDate != &quot;&quot;'> AND create_time &gt;= CONCAT(#{startDate}, ' 00:00:00')</if>",
            "<if test='endDate != null and endDate != &quot;&quot;'> AND create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>",
            "GROUP BY " + OrderMapper.NORMALIZED_PRODUCT_HOST_SQL + ", customer_country", "</script>"})
    List<Map<String, Object>> revenueOrderCountriesByDomain(@Param("startDate") String startDate,
                                                             @Param("endDate") String endDate,
                                                             @Param("ownerName") String ownerName,
                                                             @Param("teacherSuffixes") List<String> teacherSuffixes);
}
