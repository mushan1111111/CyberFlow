package com.cyberflow.admin.crawler.config;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.Trigger;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrawlerTimeZoneTest {

    @Test
    void allDefaultCrawlerTriggersUseBeijingTime() {
        QuartzConfig config = new QuartzConfig();
        ReflectionTestUtils.setField(config, "siteCron", "0 0 0 * * ?");
        ReflectionTestUtils.setField(config, "indexCron", "0 0 2 * * ?");
        ReflectionTestUtils.setField(config, "orderCron", "0 0 */6 * * ?");

        assertBeijing(config.siteCrawlTrigger());
        assertBeijing(config.siteIndexCrawlTrigger());
        assertBeijing(config.orderCrawlTrigger());
    }

    private static void assertBeijing(Trigger trigger) {
        assertEquals("Asia/Shanghai", ((CronTrigger) trigger).getTimeZone().getID());
    }
}
