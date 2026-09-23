package com.cyberflow.admin.crawler.messaging;

import com.cyberflow.admin.crawler.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SiteCrawlAccountMergeMessageTest {
    @Test
    void siteCrawlCarriesTheCanonicalAccountMap() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        TaskMessagePublisher publisher = new TaskMessagePublisher(template);
        Map<String, List<String>> mergeMap = Map.of("B-姓名", List.of("B-账号1", "B-账号2"));

        publisher.publishSiteCrawl("task", Map.of(), Map.of(), mergeMap,
                "2026-09-23T00:00:00Z", "manual");

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EXCHANGE_TASKS),
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.RK_SITE), message.capture());
        Map<?, ?> payload = (Map<?, ?>) ((Map<?, ?>) message.getValue()).get("payload");
        assertEquals(mergeMap, payload.get("user_merge_map"));
    }
}
