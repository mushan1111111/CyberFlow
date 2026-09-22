package com.cyberflow.admin.system.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationSecurityTest {

    @Test
    void credentialsAreEncryptedAndCanBeRecovered() {
        NotificationSecretCipher cipher = new NotificationSecretCipher("a-dedicated-test-key-with-32-chars");
        String encrypted = cipher.encrypt("https://example.test/secret-token");

        assertNotEquals("https://example.test/secret-token", encrypted);
        assertTrue(encrypted.startsWith("enc:v1:"));
        assertEquals("https://example.test/secret-token", cipher.decrypt(encrypted));
    }

    @Test
    void webhookValidationOnlyAllowsTheSelectedOfficialPlatformHost() {
        assertDoesNotThrow(() -> NotificationPlatform.DINGTALK.validateWebhook(
                "https://oapi.dingtalk.com/robot/send?access_token=token"));
        assertDoesNotThrow(() -> NotificationPlatform.FEISHU.validateWebhook(
                "https://open.feishu.cn/open-apis/bot/v2/hook/token"));
        assertDoesNotThrow(() -> NotificationPlatform.LARK.validateWebhook(
                "https://open.larksuite.com/open-apis/bot/v2/hook/token"));

        assertThrows(IllegalArgumentException.class, () -> NotificationPlatform.FEISHU.validateWebhook(
                "https://example.com/open-apis/bot/v2/hook/token"));
        assertThrows(IllegalArgumentException.class, () -> NotificationPlatform.DINGTALK.validateWebhook(
                "http://oapi.dingtalk.com/robot/send?access_token=token"));
        assertThrows(IllegalArgumentException.class, () -> NotificationPlatform.DINGTALK.validateWebhook(
                "https://oapi.dingtalk.com/robot/send-elsewhere?access_token=token"));
    }

    @Test
    void platformPayloadsAndSignaturesFollowTheirRobotProtocols() throws Exception {
        NotificationWebhookClient client = new NotificationWebhookClient(new ObjectMapper());
        Map<String, Object> dingtalk = client.payload(NotificationPlatform.DINGTALK, "secret", "hello", 123L);
        Map<String, Object> feishu = client.payload(NotificationPlatform.FEISHU, "secret", "hello", 123L);
        URI signed = client.signedUri(NotificationPlatform.DINGTALK,
                "https://oapi.dingtalk.com/robot/send?access_token=token", "secret", 123L);

        assertEquals("text", dingtalk.get("msgtype"));
        assertEquals("text", feishu.get("msg_type"));
        assertEquals("123", feishu.get("timestamp"));
        assertNotNull(feishu.get("sign"));
        assertTrue(signed.getRawQuery().contains("timestamp=123"));
        assertTrue(signed.getRawQuery().contains("sign="));
    }
}
