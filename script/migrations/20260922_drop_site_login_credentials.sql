-- Revert the upstream "store site login credentials" change: the WordPress
-- login URL / user / password are deliberately not persisted in this fork.
-- Guarded so it is safe whether or not the columns were ever created.
USE cyberflow;
SET NAMES utf8mb4;

SET @site_login_url_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='site_info' AND COLUMN_NAME='login_url'
);
SET @site_login_url_sql = IF(@site_login_url_exists=0,
    'SELECT 1',
    'ALTER TABLE site_info DROP COLUMN login_url');
PREPARE site_login_url_stmt FROM @site_login_url_sql;
EXECUTE site_login_url_stmt;
DEALLOCATE PREPARE site_login_url_stmt;

SET @site_wp_admin_user_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='site_info' AND COLUMN_NAME='wp_admin_user'
);
SET @site_wp_admin_user_sql = IF(@site_wp_admin_user_exists=0,
    'SELECT 1',
    'ALTER TABLE site_info DROP COLUMN wp_admin_user');
PREPARE site_wp_admin_user_stmt FROM @site_wp_admin_user_sql;
EXECUTE site_wp_admin_user_stmt;
DEALLOCATE PREPARE site_wp_admin_user_stmt;

SET @site_wp_admin_user_pwd_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='site_info' AND COLUMN_NAME='wp_admin_user_pwd'
);
SET @site_wp_admin_user_pwd_sql = IF(@site_wp_admin_user_pwd_exists=0,
    'SELECT 1',
    'ALTER TABLE site_info DROP COLUMN wp_admin_user_pwd');
PREPARE site_wp_admin_user_pwd_stmt FROM @site_wp_admin_user_pwd_sql;
EXECUTE site_wp_admin_user_pwd_stmt;
DEALLOCATE PREPARE site_wp_admin_user_pwd_stmt;
