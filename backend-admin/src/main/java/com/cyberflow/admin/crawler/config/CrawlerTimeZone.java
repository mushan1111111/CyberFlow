package com.cyberflow.admin.crawler.config;

import org.quartz.CronScheduleBuilder;

import java.time.ZoneId;
import java.util.TimeZone;

/** Shared business timezone rules for crawler schedules and task reporting. */
public final class CrawlerTimeZone {

    public static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private CrawlerTimeZone() {
    }

    public static CronScheduleBuilder cronSchedule(String expression) {
        return CronScheduleBuilder.cronSchedule(expression)
                .inTimeZone(TimeZone.getTimeZone(BEIJING));
    }
}

