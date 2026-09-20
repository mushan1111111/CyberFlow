package com.cyberflow.admin.common;

import com.cyberflow.admin.system.entity.SysOperationLog;
import com.cyberflow.admin.system.service.SysOperationLogService;
import com.cyberflow.admin.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面。
 * <p>
 * 通过环绕通知拦截所有被 {@code @RestController} 注解标记的 Controller 方法，
 * 自动记录每次请求的操作日志，包括请求方法、URL、IP、用户、耗时、执行状态等信息。
 * 日志保存采用异步方式，避免阻塞主业务线程。
 * </p>
 *
 * @author CyberFlow Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 操作日志服务，用于持久化日志记录 */
    private final SysOperationLogService logService;
    private final SysUserMapper userMapper;

    /**
     * 环绕通知：拦截所有 RestController 的公开方法，记录操作日志。
     * <p>
     * 在方法执行前提取请求信息和用户信息，执行后记录耗时和状态。
     * 若方法执行抛出异常，同样记录错误信息后重新抛出，不吞掉异常。
     * </p>
     *
     * @param joinPoint 切入点，代表被拦截的方法
     * @return 被拦截方法的原始返回值
     * @throws Throwable 被拦截方法可能抛出的任何异常
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        var startTime = System.currentTimeMillis();
        var logEntry = new SysOperationLog();
        logEntry.setCreatedAt(LocalDateTime.now());

        try {
            // 获取请求信息
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logEntry.setRequestMethod(request.getMethod());
                logEntry.setRequestUrl(request.getRequestURI());
                logEntry.setIp(request.getRemoteAddr());
                logEntry.setRequestParams(extractParams(request.getQueryString()));
            }

            // 获取用户信息
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                logEntry.setUsername(auth.getName());
                var user = userMapper.selectByUsername(auth.getName());
                if (user != null) logEntry.setUserId(user.getId());
            }

            // 推断操作类型
            String method = logEntry.getRequestMethod();
            String url = logEntry.getRequestUrl() != null ? logEntry.getRequestUrl() : "";
            logEntry.setOperation(inferOperation(method, url));
            logEntry.setModule(inferModule(url));
            logEntry.setTarget(inferTarget(url));

            Object result = joinPoint.proceed();

            logEntry.setStatus(1);
            logEntry.setCostTime(System.currentTimeMillis() - startTime);

            saveLog(logEntry);
            return result;
        } catch (Exception e) {
            logEntry.setStatus(0);
            logEntry.setErrorMsg(e.getMessage());
            logEntry.setCostTime(System.currentTimeMillis() - startTime);
            saveLog(logEntry);
            throw e;
        }
    }

    /**
     * 异步保存操作日志。
     * <p>
     * 仅记录非 GET 请求（增删改操作），GET 类型的查询请求不记录。
     * 日志保存失败时仅输出警告日志，不影响主业务流程。
     * </p>
     *
     * @param logEntry 待保存的操作日志实体
     */
    void saveLog(SysOperationLog logEntry) {
        // 只记录 CUD 操作；异步入口位于独立的 Spring Bean，确保代理生效。
        if (!"GET".equalsIgnoreCase(logEntry.getRequestMethod())) {
            logService.saveAsync(logEntry);
        }
    }

    /**
     * 根据 HTTP 方法推断操作类型。
     *
     * @param method HTTP 方法（GET/POST/PUT/DELETE）
     * @return 对应的操作类型：CREATE / UPDATE / DELETE / QUERY
     */
    private String inferOperation(String method, String url) {
        if (url.contains("/crawler/") && (url.endsWith("/start") || url.endsWith("/trigger"))) {
            return "TRIGGER_CRAWLER";
        }
        return switch (method.toUpperCase()) {
            case "POST" -> "CREATE";
            case "PUT" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "QUERY";
        };
    }

    /**
     * 根据请求 URL 推断所属业务模块。
     *
     * @param url 请求 URL 路径
     * @return 模块标识：SYSTEM / CRAWLER / DASHBOARD / UNKNOWN
     */
    private String inferModule(String url) {
        if (url.contains("/system/")) return "SYSTEM";
        if (url.contains("/auth/")) return "AUTH";
        if (url.contains("/crawler/")) return "CRAWLER";
        if (url.contains("/dashboard/")) return "DASHBOARD";
        if (url.contains("/new-site")) return "NEW_SITE";
        if (url.contains("/categories")) return "CATEGORY";
        return "UNKNOWN";
    }

    private String inferTarget(String url) {
        if (url.contains("/system/user")) return "用户管理";
        if (url.contains("/system/role")) return "角色管理";
        if (url.contains("/system/menu")) return "菜单管理";
        if (url.contains("/crawler/")) return "数据同步";
        if (url.contains("/dashboard/")) return "数据看板";
        if (url.contains("/new-site")) return "新站点管理";
        if (url.contains("/categories")) return "自定义分类";
        if (url.contains("/auth/")) return "用户认证";
        return "其他操作";
    }

    /**
     * 截取请求参数字符串，限制最大长度为 1000 字符。
     *
     * @param queryString URL 中的查询参数字符串
     * @return 截断后的参数字符串，若输入为 null 则返回 null
     */
    private String extractParams(String queryString) {
        if (queryString == null || queryString.length() > 1000) {
            return queryString != null ? queryString.substring(0, 1000) : null;
        }
        return queryString;
    }
}
