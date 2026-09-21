-- Store the three login fields returned by /adminapi/site/site/list.
USE cyberflow;
SET NAMES utf8mb4;

SET @site_login_url_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='site_info' AND COLUMN_NAME='login_url'
);
SET @site_login_url_sql = IF(@site_login_url_exists=0,
    'ALTER TABLE site_info ADD COLUMN login_url VARCHAR(1000) NULL COMMENT ''WordPress 登录地址（site/site/list.login_url）'' AFTER site_domain',
    'SELECT 1');
PREPARE site_login_url_stmt FROM @site_login_url_sql;
EXECUTE site_login_url_stmt;
DEALLOCATE PREPARE site_login_url_stmt;

SET @site_wp_admin_user_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='site_info' AND COLUMN_NAME='wp_admin_user'
);
SET @site_wp_admin_user_sql = IF(@site_wp_admin_user_exists=0,
    'ALTER TABLE site_info ADD COLUMN wp_admin_user VARCHAR(255) NULL COMMENT ''WordPress 登录账户（site/site/list.wp_admin_user）'' AFTER login_url',
    'SELECT 1');
PREPARE site_wp_admin_user_stmt FROM @site_wp_admin_user_sql;
EXECUTE site_wp_admin_user_stmt;
DEALLOCATE PREPARE site_wp_admin_user_stmt;

SET @site_wp_admin_user_pwd_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='site_info' AND COLUMN_NAME='wp_admin_user_pwd'
);
SET @site_wp_admin_user_pwd_sql = IF(@site_wp_admin_user_pwd_exists=0,
    'ALTER TABLE site_info ADD COLUMN wp_admin_user_pwd VARCHAR(500) NULL COMMENT ''WordPress 登录密码（site/site/list.wp_admin_user_pwd）'' AFTER wp_admin_user',
    'SELECT 1');
PREPARE site_wp_admin_user_pwd_stmt FROM @site_wp_admin_user_pwd_sql;
EXECUTE site_wp_admin_user_pwd_stmt;
DEALLOCATE PREPARE site_wp_admin_user_pwd_stmt;
