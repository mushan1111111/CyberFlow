-- Clean legacy RBAC links before enforcing referential integrity.
DELETE ur FROM sys_user_role ur LEFT JOIN sys_user u ON u.id = ur.user_id WHERE u.id IS NULL;
DELETE ur FROM sys_user_role ur LEFT JOIN sys_role r ON r.id = ur.role_id WHERE r.id IS NULL;
DELETE rm FROM sys_role_menu rm LEFT JOIN sys_role r ON r.id = rm.role_id WHERE r.id IS NULL;
DELETE rm FROM sys_role_menu rm LEFT JOIN sys_menu m ON m.id = rm.menu_id WHERE m.id IS NULL;

-- Complete useful audit columns for existing entries when they can be inferred.
UPDATE sys_operation_log l
INNER JOIN sys_user u ON u.username = l.username
SET l.user_id = u.id
WHERE l.user_id IS NULL;

UPDATE sys_operation_log
SET target = CASE
    WHEN request_url LIKE '%/system/user%' THEN '用户管理'
    WHEN request_url LIKE '%/system/role%' THEN '角色管理'
    WHEN request_url LIKE '%/system/menu%' THEN '菜单管理'
    WHEN request_url LIKE '%/crawler/%' THEN '数据同步'
    WHEN request_url LIKE '%/dashboard/%' THEN '数据看板'
    WHEN request_url LIKE '%/new-site%' THEN '新站点管理'
    WHEN request_url LIKE '%/categories%' THEN '自定义分类'
    WHEN request_url LIKE '%/auth/%' THEN '用户认证'
    ELSE '其他操作'
END
WHERE target IS NULL OR target = '';

UPDATE sys_operation_log SET module = 'AUTH'
WHERE module = 'UNKNOWN' AND request_url LIKE '%/auth/%';
UPDATE sys_operation_log SET module = 'NEW_SITE'
WHERE module = 'UNKNOWN' AND request_url LIKE '%/new-site%';
UPDATE sys_operation_log SET module = 'CATEGORY'
WHERE module = 'UNKNOWN' AND request_url LIKE '%/categories%';

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
           WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_sys_user_role_user'),
    'SELECT 1',
    'ALTER TABLE sys_user_role ADD CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
           WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_sys_user_role_role'),
    'SELECT 1',
    'ALTER TABLE sys_user_role ADD CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
           WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_sys_role_menu_role'),
    'SELECT 1',
    'ALTER TABLE sys_role_menu ADD CONSTRAINT fk_sys_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
           WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_sys_role_menu_menu'),
    'SELECT 1',
    'ALTER TABLE sys_role_menu ADD CONSTRAINT fk_sys_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
