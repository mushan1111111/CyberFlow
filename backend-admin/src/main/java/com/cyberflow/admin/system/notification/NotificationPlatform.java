package com.cyberflow.admin.system.notification;

import java.net.URI;
import java.util.Locale;

public enum NotificationPlatform {
    DINGTALK("钉钉", "oapi.dingtalk.com", "/robot/send"),
    FEISHU("飞书", "open.feishu.cn", "/open-apis/bot/v2/hook/"),
    LARK("Lark", "open.larksuite.com", "/open-apis/bot/v2/hook/");

    private final String label;
    private final String host;
    private final String pathPrefix;

    NotificationPlatform(String label, String host, String pathPrefix) {
        this.label = label;
        this.host = host;
        this.pathPrefix = pathPrefix;
    }

    public String label() {
        return label;
    }

    public static NotificationPlatform parse(String value) {
        try {
            return valueOf(String.valueOf(value).trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("通知平台仅支持钉钉、飞书或 Lark");
        }
    }

    public URI validateWebhook(String value) {
        final URI uri;
        try {
            uri = URI.create(String.valueOf(value).trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Webhook 地址格式不正确");
        }
        String actualHost = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean validPort = uri.getPort() == -1 || uri.getPort() == 443;
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !host.equals(actualHost)
                || !validPort || uri.getUserInfo() != null || uri.getFragment() != null
                || uri.getPath() == null || !validPath(uri.getPath())) {
            throw new IllegalArgumentException(label + " Webhook 必须使用官方 HTTPS 地址 " + host);
        }
        if (this == DINGTALK && (uri.getQuery() == null
                || !uri.getQuery().matches("(?:^|.*&)access_token=[^&]+(?:&.*|$)"))) {
            throw new IllegalArgumentException("钉钉 Webhook 缺少 access_token");
        }
        return uri;
    }

    private boolean validPath(String path) {
        return this == DINGTALK ? pathPrefix.equals(path) : path.startsWith(pathPrefix);
    }
}
