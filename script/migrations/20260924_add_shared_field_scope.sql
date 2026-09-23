ALTER TABLE sys_user
    ADD COLUMN shared_data_owners VARCHAR(1000)
        COMMENT '允许查看的其他成员管理员名称，用逗号分隔' AFTER data_owner,
    ADD COLUMN shared_data_fields TEXT
        COMMENT '其他成员数据字段权限编码' AFTER shared_data_owners;
