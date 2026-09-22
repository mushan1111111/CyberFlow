package com.cyberflow.admin.system.service;

import com.cyberflow.admin.system.entity.SysNotificationChannel;
import com.cyberflow.admin.system.notification.NotificationPlatform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysNotificationTemplateTest {

    @Test
    void configuredVariablesRenderIntoTheFinalPlainTextMessage() {
        SysNotificationService service = new SysNotificationService(null, null, null);
        SysNotificationChannel channel = new SysNotificationChannel();
        channel.setName("订单告警群");
        channel.setMessageTemplate("{{title}}\n{{content}}\n{{platform}}｜{{channel}}｜{{time}}");

        String message = service.render(channel, NotificationPlatform.FEISHU, "同步失败", "订单接口超时");

        assertTrue(message.contains("同步失败\n订单接口超时"));
        assertTrue(message.contains("飞书｜订单告警群｜"));
        assertFalse(message.contains("{{time}}"));
    }
}
