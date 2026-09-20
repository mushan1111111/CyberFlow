package com.cyberflow.admin.crawler.siteaccount.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人站点账号同步配置，对应 site_account_sync。
 * <p>
 * 建站平台的 site/site/list 接口只返回「当前登录账号」名下站点的主题与分类，
 * 因此全局账号无法补齐所有人的这两个字段。这里保存每位成员自己的平台账号，
 * 同步时以该账号登录，只合并其本人站点的主题、分类等信息。
 * </p>
 */
@Data
@TableName("site_account_sync")
public class SiteAccountSync {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 本地系统用户 ID，用于隔离「只能看自己的配置」 */
    private Long userId;

    /** 建站平台登录账号 */
    private String remoteUsername;

    /** 建站平台登录密码 */
    private String remotePassword;

    /** 对应 site_info.admin_name，用于进一步限定同步范围 */
    private String ownerName;

    /** 1 启用，0 停用 */
    private Integer enabled;

    private LocalDateTime lastSyncedAt;

    private String lastStatus;

    private String lastMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
