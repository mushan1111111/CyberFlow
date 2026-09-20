package com.cyberflow.admin.crawler.messaging;

import com.cyberflow.admin.crawler.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 任务消息发布器。
 * <p>
 * 负责将各类爬虫任务封装为标准消息格式，通过 RabbitMQ 发送到对应的队列。
 * 每条消息包含唯一的 task_id、任务类型、时间戳和携带参数（payload）。
 * </p>
 *
 * <h3>消息格式</h3>
 * <pre>
 * {
 *     "task_id": "uuid",
 *     "type": "site_crawl|order_crawl|product_crawl",
 *     "trigger": "cron|manual",
 *     "timestamp": "ISO-8601",
 *     "payload": { ... }
 * }
 * </pre>
 *
 * @author CyberFlow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessagePublisher {

    /** RabbitMQ 模板，用于发送消息 */
    private final RabbitTemplate rabbitTemplate;

    /** Allocate the id before publishing so the history row can exist first. */
    public String createTaskId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 发布站点爬取任务消息。
     *
     * @param username      目标站点登录用户名
     * @param password      目标站点登录密码
     * @param lastUpdatedAt 增量爬取起始时间（上次更新时间的 ISO 字符串）
     * @return 生成的任务唯一标识（UUID）
     */
    public String publishSiteCrawl(Map<String, Object> platform, Map<String, Object> strategy, String lastUpdatedAt, String trigger) {
        return publishSiteCrawl(createTaskId(), platform, strategy, lastUpdatedAt, trigger);
    }

    public String publishSiteCrawl(String taskId, Map<String, Object> platform, Map<String, Object> strategy,
                                   String lastUpdatedAt, String trigger) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platform", platform);
        payload.put("strategy", strategy);
        payload.put("cursor", Map.of("last_updated_at", lastUpdatedAt));
        Map<String, Object> message = Map.of(
            "task_id", taskId,
            "type", "site_crawl",
            "trigger", trigger,
            "timestamp", Instant.now().toString(),
            "payload", payload
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_TASKS, RabbitMQConfig.RK_SITE, message);
        log.info("Published site crawl task: {}", taskId);
        return taskId;
    }

    /**
     * 发布站点索引/收录任务消息。
     *
     * @param username        目标站点登录用户名
     * @param password        目标站点登录密码
     * @param lastRecordedAt  增量收录起始时间（上次收录时间的 ISO 字符串）
     * @return 生成的任务唯一标识（UUID）
     */
    public String publishSiteIndexCrawl(Map<String, Object> platform, Map<String, Object> strategy, String lastRecordedAt, String trigger) {
        return publishSiteIndexCrawl(createTaskId(), platform, strategy, lastRecordedAt, trigger);
    }

    public String publishSiteIndexCrawl(String taskId, Map<String, Object> platform, Map<String, Object> strategy,
                                        String lastRecordedAt, String trigger) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platform", platform);
        payload.put("strategy", strategy);
        payload.put("cursor", Map.of("last_recorded_at", lastRecordedAt));
        Map<String, Object> message = Map.of(
            "task_id", taskId,
            "type", "site_index",
            "trigger", trigger,
            "timestamp", Instant.now().toString(),
            "payload", payload
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_TASKS, RabbitMQConfig.RK_SITE, message);
        log.info("Published site index crawl task: {}", taskId);
        return taskId;
    }

    /**
     * 发布「个人站点账号」同步任务消息。
     * <p>
     * 与全局 site_crawl 的区别：采集端用消息里 person 自己的账号登录建站平台，
     * 只处理该账号可见的站点，并且只做字段合并、不做镜像删除，
     * 从而补齐 site/site/list 受权限限制拿不到的主题与分类。
     * </p>
     *
     * @param platform  连接信息（用户名/密码已被替换为该成员自己的账号）
     * @param strategy  站点分页等策略
     * @param ownerName 可选的负责人名称，用于把结果进一步限定在该成员名下
     * @param trigger   触发方式
     * @return 生成的任务唯一标识（UUID）
     */
    public String publishSiteAccountCrawl(String taskId, Map<String, Object> platform, Map<String, Object> strategy,
                                          String ownerName, String trigger) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platform", platform);
        payload.put("strategy", strategy);
        payload.put("owner_name", ownerName);
        Map<String, Object> message = Map.of(
            "task_id", taskId,
            "type", "site_account",
            "trigger", trigger,
            "timestamp", Instant.now().toString(),
            "payload", payload
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_TASKS, RabbitMQConfig.RK_SITE, message);
        log.info("Published site account crawl task: {}", taskId);
        return taskId;
    }

    /**
     * 发布订单爬取任务消息。
     *
     * @param maxOrderId 增量爬取的起始订单 ID（作为光标使用）
     * @return 生成的任务唯一标识（UUID）
     */
    public String publishOrderCrawl(Map<String, Object> platform, Map<String, Object> strategy,
                                    String maxOrderId, String trigger, String userGroup) {
        return publishOrderCrawl(createTaskId(), platform, strategy, maxOrderId, trigger, userGroup);
    }

    public String publishOrderCrawl(String taskId, Map<String, Object> platform, Map<String, Object> strategy,
                                    String maxOrderId, String trigger, String userGroup) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platform", platform);
        payload.put("strategy", strategy);
        payload.put("cursor", Map.of("max_order_id", maxOrderId));
        payload.put("user_group", userGroup);
        Map<String, Object> message = Map.of(
            "task_id", taskId,
            "type", "order_crawl",
            "trigger", trigger,
            "timestamp", Instant.now().toString(),
            "payload", payload
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_TASKS, RabbitMQConfig.RK_ORDER, message);
        log.info("Published order crawl task: {}", taskId);
        return taskId;
    }

    /**
     * 发布商品爬取任务消息。
     *
     * @param siteConfigId 站点配置 ID
     * @param domain       目标站点域名
     * @param type         站点类型
     * @param category     商品分类
     * @param productRole  商品标签：main-主产品，supplement-补充产品
     * @param triggeredBy  触发者用户 ID
     * @return 生成的任务唯一标识（UUID）
     */
    public String publishProductCrawl(Long siteConfigId, String domain, String type,
                                      String category, String productRole, Long triggeredBy) {
        return publishProductCrawl(createTaskId(), siteConfigId, domain, type, category, productRole, triggeredBy);
    }

    public String publishProductCrawl(String taskId, Long siteConfigId, String domain, String type,
                                      String category, String productRole, Long triggeredBy) {
        return publishProductCrawl(taskId, siteConfigId, domain, type, category, productRole, triggeredBy, Map.of());
    }

    public String publishProductCrawl(String taskId, Long siteConfigId, String domain, String type,
                                      String category, String productRole, Long triggeredBy,
                                      Map<String, Object> crawlOptions) {
        Map<String, Object> message = Map.of(
            "task_id", taskId,
            "type", "product_crawl",
            "trigger", "manual",
            "triggered_by", String.valueOf(triggeredBy),
            "timestamp", Instant.now().toString(),
            "payload", Map.of(
                "site_config_id", siteConfigId,
                "domain", domain,
                "type", type,
                "category", category,
                "product_role", productRole == null || productRole.isBlank() ? "main" : productRole,
                "crawl_options", crawlOptions
            )
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_TASKS, RabbitMQConfig.RK_PRODUCT, message);
        log.info("Published product crawl task: {}", taskId);
        return taskId;
    }
}
