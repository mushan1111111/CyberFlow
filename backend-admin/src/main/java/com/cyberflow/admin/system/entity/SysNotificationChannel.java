package com.cyberflow.admin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** Encrypted webhook configuration for an administrator-managed chat robot. */
@Data
@TableName("sys_notification_channel")
public class SysNotificationChannel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String platform;

    @JsonIgnore
    private String webhookUrl;

    @JsonIgnore
    private String signingSecret;

    private String messageTemplate;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
