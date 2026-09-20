package com.cyberflow.admin.crawler.siteaccount.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyberflow.admin.crawler.config.service.CrawlerConfigService;
import com.cyberflow.admin.crawler.messaging.TaskMessagePublisher;
import com.cyberflow.admin.crawler.siteaccount.entity.SiteAccountSync;
import com.cyberflow.admin.crawler.siteaccount.mapper.SiteAccountSyncMapper;
import com.cyberflow.admin.crawler.task.entity.TaskHistory;
import com.cyberflow.admin.crawler.task.service.TaskHistoryService;
import com.cyberflow.admin.system.entity.SysUser;
import com.cyberflow.admin.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人站点账号同步服务。
 * <p>
 * 建站平台 site/site/list 的主题与分类受账号权限限制，单次全局同步只能拿到
 * 「当前号主」的数据。这里让每位成员录入自己的平台账号，同步时以该账号登录，
 * 只拉取并合并本人站点的主题、分类、站点标签等字段。
 * </p>
 *
 * <p>安全约束：</p>
 * <ul>
 *   <li>每个人只能看到、修改、删除、触发自己的账号配置（管理员例外，仅用于排查）；</li>
 *   <li>密码对外一律返回掩码，只有重新输入才会被覆盖；</li>
 *   <li>下发任务时只覆盖 platform 的账号密码，其余连接信息沿用全局 Admin API 配置。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteAccountSyncService {

    /** 密码掩码，与前端「未修改」占位符保持一致 */
    public static final String MASK = "******";

    /** 任务历史中的类型标识，长度受 task_history.type 限制 */
    private static final String TASK_TYPE = "site_account";

    private final SiteAccountSyncMapper accountMapper;
    private final SysUserMapper userMapper;
    private final CrawlerConfigService crawlerConfigService;
    private final TaskMessagePublisher publisher;
    private final TaskHistoryService taskHistoryService;

    /**
     * 查询账号列表：普通用户只返回自己的配置，管理员返回全部以便排查。
     *
     * @return 账号视图列表（密码已掩码）
     */
    public List<Map<String, Object>> list() {
        Long userId = currentUserId();
        boolean administrator = isAdministrator(userId);
        List<SiteAccountSync> rows = administrator
                ? accountMapper.selectList(new LambdaQueryWrapper<SiteAccountSync>()
                        .orderByDesc(SiteAccountSync::getId))
                : accountMapper.selectList(new LambdaQueryWrapper<SiteAccountSync>()
                        .eq(SiteAccountSync::getUserId, userId)
                        .orderByDesc(SiteAccountSync::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SiteAccountSync row : rows) {
            result.add(toView(row, administrator));
        }
        return result;
    }

    /**
     * 新增或更新一条账号配置。带 id 时更新指定记录，否则按
     * （当前用户 + 平台账号）做 upsert，保证一个人一个平台账号只有一条记录。
     *
     * @param body 包含 id（可选）、remoteUsername、remotePassword、ownerName、enabled
     * @return 保存后的账号视图
     */
    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        Long userId = currentUserId();
        boolean administrator = isAdministrator(userId);
        SiteAccountSync entity;
        Object rawId = body.get("id");
        if (rawId != null && !String.valueOf(rawId).isBlank()) {
            entity = requireOwned(Long.parseLong(String.valueOf(rawId)), userId, administrator);
        } else {
            String remoteUsername = text(body.get("remoteUsername"));
            if (remoteUsername.isBlank()) {
                throw new IllegalArgumentException("请填写建站平台账号");
            }
            entity = accountMapper.selectOne(new LambdaQueryWrapper<SiteAccountSync>()
                    .eq(SiteAccountSync::getUserId, userId)
                    .eq(SiteAccountSync::getRemoteUsername, remoteUsername));
            if (entity == null) {
                entity = new SiteAccountSync();
                entity.setUserId(userId);
                entity.setRemoteUsername(remoteUsername);
                entity.setEnabled(1);
            }
        }

        String username = text(body.get("remoteUsername"));
        if (!username.isBlank()) {
            entity.setRemoteUsername(username);
        }
        String password = text(body.get("remotePassword"));
        if (!password.isBlank() && !MASK.equals(password)) {
            entity.setRemotePassword(password);
        } else if (entity.getRemotePassword() == null || entity.getRemotePassword().isBlank()) {
            throw new IllegalArgumentException("请填写建站平台密码");
        }
        if (body.containsKey("ownerName")) {
            entity.setOwnerName(text(body.get("ownerName")));
        }
        if (body.get("enabled") != null) {
            entity.setEnabled(toFlag(body.get("enabled")));
        } else if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        entity.setUpdatedAt(LocalDateTime.now());

        if (entity.getId() == null) {
            accountMapper.insert(entity);
        } else {
            accountMapper.updateById(entity);
        }
        return toView(entity, administrator);
    }

    /**
     * 删除自己的账号配置。
     *
     * @param id 配置 ID
     */
    @Transactional
    public void delete(Long id) {
        Long userId = currentUserId();
        accountMapper.deleteById(requireOwned(id, userId, isAdministrator(userId)).getId());
    }

    /**
     * 触发一次「只用该账号」的站点同步任务。
     * <p>
     * 采集端会用这个账号登录建站平台，只处理该账号可见的站点，并且只做字段合并、
     * 不做镜像删除，因此不会影响其他人的站点数据。
     * </p>
     *
     * @param id 配置 ID
     * @return 包含 task_id 的结果
     */
    public Map<String, Object> triggerSync(Long id) {
        Long userId = currentUserId();
        SiteAccountSync account = requireOwned(id, userId, isAdministrator(userId));
        if (account.getEnabled() == null || account.getEnabled() != 1) {
            throw new IllegalArgumentException("该账号已停用，请先启用后再同步");
        }
        return dispatch(account, "manual");
    }

    /**
     * 为所有已启用的账号各下发一次同步任务（供计划任务与「立即执行」使用）。
     * <p>
     * 某个账号已在跑任务时跳过它，不会让整批失败。
     * </p>
     *
     * @param trigger 触发方式：cron / manual
     * @return 下发数量、跳过数量与任务 ID 列表
     */
    public Map<String, Object> triggerAll(String trigger) {
        List<SiteAccountSync> rows = accountMapper.selectList(
                new LambdaQueryWrapper<SiteAccountSync>().eq(SiteAccountSync::getEnabled, 1));
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("尚未配置任何已启用的站点账号");
        }
        List<String> taskIds = new ArrayList<>();
        int skipped = 0;
        for (SiteAccountSync row : rows) {
            try {
                taskIds.add(String.valueOf(dispatch(row, trigger).get("task_id")));
            } catch (RuntimeException ex) {
                skipped += 1;
                log.warn("Skip site account sync: accountId={}, reason={}", row.getId(), ex.getMessage());
            }
        }
        if (taskIds.isEmpty()) {
            throw new IllegalStateException("所有站点账号都有任务在执行或暂停，请等待任务结束后再试");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dispatched", taskIds.size());
        result.put("skipped", skipped);
        result.put("task_ids", taskIds);
        result.put("status", "Task dispatched");
        return result;
    }

    /**
     * 下发单个账号的同步任务，不校验归属（供计划任务调用，此时没有登录用户）。
     */
    @Transactional
    public Map<String, Object> dispatch(SiteAccountSync account, String trigger) {
        if (account.getEnabled() == null || account.getEnabled() != 1) {
            throw new IllegalArgumentException("该账号已停用");
        }
        String scope = "account-" + account.getId();
        if (taskHistoryService.hasActiveTask(TASK_TYPE, scope)) {
            throw new IllegalArgumentException("该账号已有同步任务在执行或暂停，请等待任务结束后再试");
        }

        Map<String, Object> platform = new LinkedHashMap<>(crawlerConfigService.getAdminPlatform());
        platform.put("username", account.getRemoteUsername());
        platform.put("password", account.getRemotePassword());

        String taskId = publisher.createTaskId();
        TaskHistory history = new TaskHistory();
        history.setTaskId(taskId);
        history.setType(TASK_TYPE);
        history.setTriggerType(trigger);
        history.setTriggeredBy(scope);
        history.setStatus("PENDING");
        taskHistoryService.save(history);
        try {
            publisher.publishSiteAccountCrawl(taskId, platform, crawlerConfigService.getSiteStrategy(),
                    account.getOwnerName(), trigger);
        } catch (RuntimeException ex) {
            taskHistoryService.markDispatchFailed(taskId, ex.getMessage());
            throw ex;
        }
        log.info("Dispatched personal site sync: accountId={}, taskId={}", account.getId(), taskId);
        return Map.of("task_id", taskId, "account_id", account.getId(), "status", "Task dispatched");
    }

    private SiteAccountSync requireOwned(Long id, Long userId, boolean administrator) {
        SiteAccountSync account = accountMapper.selectById(id);
        if (account == null) {
            throw new IllegalArgumentException("站点账号配置不存在");
        }
        if (!administrator && !userId.equals(account.getUserId())) {
            throw new IllegalArgumentException("只能操作自己的站点账号配置");
        }
        return account;
    }

    private Map<String, Object> toView(SiteAccountSync row, boolean administrator) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.getId());
        item.put("remoteUsername", row.getRemoteUsername());
        item.put("remotePassword", MASK);
        item.put("ownerName", row.getOwnerName());
        item.put("enabled", row.getEnabled() != null && row.getEnabled() == 1);
        item.put("lastSyncedAt", row.getLastSyncedAt());
        item.put("lastStatus", row.getLastStatus());
        item.put("lastMessage", row.getLastMessage());
        item.put("owned", administrator || row.getUserId() != null && row.getUserId().equals(currentUserIdSafe()));
        if (administrator) {
            SysUser owner = row.getUserId() == null ? null : userMapper.selectById(row.getUserId());
            item.put("ownerUsername", owner == null ? null : owner.getUsername());
        }
        return item;
    }

    private Long currentUserIdSafe() {
        try {
            return currentUserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("当前用户未认证");
        }
        SysUser user = userMapper.selectByUsername(authentication.getName());
        if (user == null) {
            throw new IllegalStateException("当前用户不存在");
        }
        return user.getId();
    }

    private boolean isAdministrator(Long userId) {
        return userMapper.selectRoleCodesByUserId(userId).stream()
                .anyMatch("ROLE_ADMIN"::equalsIgnoreCase);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int toFlag(Object value) {
        if (value instanceof Boolean flag) {
            return flag ? 1 : 0;
        }
        String raw = text(value);
        return raw.equals("1") || raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("on") ? 1 : 0;
    }
}
