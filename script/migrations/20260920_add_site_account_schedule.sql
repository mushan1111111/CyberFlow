-- Weekly schedule for the personal site-account sync.
-- site/site/list only returns theme/category for the logged-in account, so each
-- enabled member account has to be crawled separately.  One weekly run is
-- enough to keep theme and category fresh without hammering the platform.

INSERT IGNORE INTO crawler_schedule_config (task_type, cron_expression, enabled)
VALUES ('site_account', '0 30 3 ? * MON', 1);
