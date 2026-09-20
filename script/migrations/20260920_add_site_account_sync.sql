-- Personal site-account synchronization.
--
-- /adminapi/site/site/list only exposes theme_name / product_category for the
-- sites owned by the logged-in account, so one global crawl cannot fill those
-- two fields for everybody.  This table stores each member's own site-platform
-- credentials so a scoped crawl can log in as them and merge theme/category
-- for their own sites only.

CREATE TABLE IF NOT EXISTS site_account_sync (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL COMMENT '本地系统用户 ID',
    remote_username VARCHAR(128) NOT NULL COMMENT '建站平台登录账号',
    remote_password VARCHAR(255) NOT NULL COMMENT '建站平台登录密码',
    owner_name      VARCHAR(100) COMMENT '对应 site_info.admin_name，留空则以平台返回为准',
    enabled         TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    last_synced_at  DATETIME COMMENT '最近一次同步完成时间',
    last_status     VARCHAR(20) COMMENT '最近一次同步结果',
    last_message    VARCHAR(255) COMMENT '最近一次同步说明',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_account_sync_user (user_id, remote_username),
    INDEX idx_site_account_sync_owner (owner_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人站点账号同步配置';

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, perms, path, component, icon, sort_order, status)
VALUES (37, 2, '站点账号同步', 1, 'crawler:site:sync', '/crawler/site-account', 'crawler/SiteAccountSync', 'Connection', 7, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), menu_name=VALUES(menu_name), menu_type=VALUES(menu_type),
    perms=VALUES(perms), path=VALUES(path), component=VALUES(component), icon=VALUES(icon),
    sort_order=VALUES(sort_order), status=1;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 37), (2, 37), (3, 37);
