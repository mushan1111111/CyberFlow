CREATE TABLE IF NOT EXISTS sys_notification_channel (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    webhook_url VARCHAR(2000) NOT NULL,
    signing_secret VARCHAR(2000) NULL,
    message_template VARCHAR(4000) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notification_enabled (enabled, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_menu
    (id, parent_id, menu_name, menu_type, perms, path, component, icon, sort_order, status)
VALUES
    (73, 3, '通知配置', 1, 'system:notification:view', '/system/notification', 'system/NotificationConfig', 'Bell', 4, 1),
    (74, 73, '管理通知机器人', 2, 'system:notification:manage', NULL, NULL, NULL, 1, 1),
    (75, 73, '测试通知机器人', 2, 'system:notification:test', NULL, NULL, NULL, 2, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), menu_name=VALUES(menu_name), menu_type=VALUES(menu_type),
    perms=VALUES(perms), path=VALUES(path), component=VALUES(component), icon=VALUES(icon),
    sort_order=VALUES(sort_order), status=VALUES(status);

UPDATE sys_menu SET sort_order=5 WHERE id=34;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code='ROLE_ADMIN' AND m.id IN (73, 74, 75);
