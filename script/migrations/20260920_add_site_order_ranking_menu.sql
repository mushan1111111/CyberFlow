-- 站点订单排行：新增独立菜单（数据看板 → 站点订单排行）
-- 页面按站点维度统计订单，支持域名/负责人/主题/订单数/金额/收录/商品数的组合筛选。

INSERT INTO sys_menu
    (id, parent_id, menu_name, menu_type, perms, path, component, icon, sort_order, status)
VALUES
    (17, 1, '站点订单排行', 1, 'dashboard:site:view', '/dashboard/site-order-ranking',
     'dashboard/SiteOrderRanking', 'TrendCharts', 5, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_type = VALUES(menu_type),
    perms = VALUES(perms),
    path = VALUES(path),
    component = VALUES(component),
    icon = VALUES(icon),
    sort_order = VALUES(sort_order),
    status = 1;

-- 授予超级管理员(1)、运营人员(2)与只读角色(3)
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 17), (2, 17), (3, 17);
