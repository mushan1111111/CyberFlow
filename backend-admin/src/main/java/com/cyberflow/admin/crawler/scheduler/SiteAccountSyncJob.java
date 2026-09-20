package com.cyberflow.admin.crawler.scheduler;

import com.cyberflow.admin.crawler.config.service.CrawlerConfigService;
import com.cyberflow.admin.crawler.siteaccount.service.SiteAccountSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Quartz 定时「个人站点账号同步」任务。
 * <p>
 * site/site/list 的主题与分类只有本人账号拿得到，所以要按每个人的账号各跑一次。
 * 本任务遍历所有已启用的站点账号，逐个下发 site_account 同步任务；
 * 每个账号只会合并它自己可见的站点，互不干扰。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class SiteAccountSyncJob implements Job {

    private final SiteAccountSyncService siteAccountSyncService;
    private final CrawlerConfigService crawlerConfigService;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Quartz triggered: site account sync job");
        if (!crawlerConfigService.isScheduleEnabled("site_account")) {
            log.info("Site account sync schedule is disabled");
            return;
        }
        try {
            siteAccountSyncService.triggerAll("cron");
        } catch (RuntimeException ex) {
            // No configured account is a normal state, not a job failure.
            log.warn("Site account sync schedule skipped: {}", ex.getMessage());
            return;
        }
        crawlerConfigService.markTriggered("site_account");
    }
}
