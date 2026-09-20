SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_notification_channel' AND COLUMN_NAME='message_template'),
    'SELECT 1',
    'ALTER TABLE sys_notification_channel ADD COLUMN message_template VARCHAR(4000) NULL AFTER signing_secret'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_notification_channel
SET message_template='【{{title}}】\n{{content}}\n\n渠道：{{channel}}\n时间：{{time}}（北京时间）'
WHERE message_template IS NULL OR message_template='';

