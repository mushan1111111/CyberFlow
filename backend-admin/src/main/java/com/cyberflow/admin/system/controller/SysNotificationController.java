package com.cyberflow.admin.system.controller;

import com.cyberflow.admin.common.Result;
import com.cyberflow.admin.system.service.SysNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system/notification")
@RequiredArgsConstructor
public class SysNotificationController {

    private final SysNotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:notification:view')")
    public Result<List<SysNotificationService.ChannelView>> list() {
        return Result.ok(notificationService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:notification:manage')")
    public Result<SysNotificationService.ChannelView> create(@RequestBody SysNotificationService.ChannelRequest request) {
        return Result.ok(notificationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:notification:manage')")
    public Result<SysNotificationService.ChannelView> update(@PathVariable Long id,
                                                              @RequestBody SysNotificationService.ChannelRequest request) {
        return Result.ok(notificationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:notification:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('system:notification:test')")
    public Result<Void> test(@PathVariable Long id,
                             @RequestBody(required = false) SysNotificationService.TestMessageRequest request) {
        notificationService.sendTest(id, request);
        return Result.ok();
    }
}
