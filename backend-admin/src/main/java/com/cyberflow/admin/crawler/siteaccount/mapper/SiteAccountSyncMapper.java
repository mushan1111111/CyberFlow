package com.cyberflow.admin.crawler.siteaccount.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyberflow.admin.crawler.siteaccount.entity.SiteAccountSync;
import org.apache.ibatis.annotations.Mapper;

/**
 * 个人站点账号同步配置数据访问接口。
 */
@Mapper
public interface SiteAccountSyncMapper extends BaseMapper<SiteAccountSync> {
}
