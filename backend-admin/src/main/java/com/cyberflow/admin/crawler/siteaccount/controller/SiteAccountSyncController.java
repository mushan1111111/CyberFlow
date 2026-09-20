package com.cyberflow.admin.crawler.siteaccount.controller;

import com.cyberflow.admin.common.Result;
import com.cyberflow.admin.crawler.siteaccount.service.SiteAccountSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个人站点账号同步 REST 控制器。
 * <p>
 * 建站平台的 site/site/list 只返回当前登录账号名下站点的主题与分类，
 * 因此提供本接口让每位成员维护自己的平台账号，并单独触发一次只覆盖本人站点的同步。
 * </p>
 *
 * <h3>权限列表</h3>
 * <ul>
 *   <li>crawler:site:sync - 查看/维护/触发自己的站点账号同步</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/crawler/site-account")
@RequiredArgsConstructor
public class SiteAccountSyncController {

    private final SiteAccountSyncService siteAccountSyncService;

    /**
     * 查询账号列表（普通用户只返回自己的配置）。
     *
     * @return 账号列表，密码已掩码
     */
    @GetMapping
    @PreAuthorize("hasAuthority('crawler:site:sync')")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(siteAccountSyncService.list());
    }

    /**
     * 新增或更新一条账号配置。
     *
     * @param body 账号信息
     * @return 保存后的账号
     */
    @PostMapping
    @PreAuthorize("hasAuthority('crawler:site:sync')")
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        return Result.ok(siteAccountSyncService.save(body));
    }

    /**
     * 删除账号配置。
     *
     * @param id 配置 ID
     * @return 空结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crawler:site:sync')")
    public Result<?> delete(@PathVariable Long id) {
        siteAccountSyncService.delete(id);
        return Result.ok();
    }

    /**
     * 用该账号触发一次站点同步，只合并该账号可见站点的主题、分类等字段。
     *
     * @param id 配置 ID
     * @return 包含 task_id 的执行结果
     */
    @PostMapping("/{id}/sync")
    @PreAuthorize("hasAuthority('crawler:site:sync')")
    public Result<Map<String, Object>> sync(@PathVariable Long id) {
        return Result.ok(siteAccountSyncService.triggerSync(id));
    }
}
