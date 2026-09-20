package com.cyberflow.admin.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyberflow.admin.system.entity.SysMenu;
import com.cyberflow.admin.system.mapper.SysMenuMapper;
import com.cyberflow.admin.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 系统菜单/权限业务服务。
 * <p>
 * 继承 MyBatis-Plus 的 {@link ServiceImpl}，提供菜单的 CRUD 操作。
 * 核心功能包括菜单树的构建和用户可见菜单的查询。
 * </p>
 *
 * @author CyberFlow Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    /** 菜单 Mapper，用于执行自定义 SQL（按用户查询菜单） */
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public boolean createMenu(SysMenu menu) {
        validateMenu(menu);
        return save(menu);
    }

    public boolean updateMenu(SysMenu menu) {
        if (menu == null || menu.getId() == null) {
            throw new IllegalArgumentException("菜单不存在");
        }
        SysMenu previous = getById(menu.getId());
        if (previous == null) throw new IllegalArgumentException("菜单不存在");
        validateMenu(menu);
        if (isCoreSystemMenu(previous) && (menu.getStatus() == 0
                || !Objects.equals(previous.getPerms(), menu.getPerms())
                || !Objects.equals(previous.getPath(), menu.getPath()))) {
            throw new IllegalArgumentException("系统管理核心菜单不能停用或更改权限路径");
        }
        assertNoCycle(menu.getId(), menu.getParentId());
        return updateById(menu);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteMenu(Long menuId) {
        if (menuId == null || getById(menuId) == null) throw new IllegalArgumentException("菜单不存在");
        List<SysMenu> all = list();
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        collectDescendants(menuId, all, ids);
        if (all.stream().anyMatch(menu -> ids.contains(menu.getId()) && isCoreSystemMenu(menu))) {
            throw new IllegalArgumentException("系统管理核心菜单不能删除");
        }
        ids.forEach(roleMenuMapper::deleteByMenuId);
        removeByIds(ids);
    }

    /**
     * 获取完整的菜单树结构。
     * <p>
     * 查询所有菜单，按 parentId 进行分组递归构建树形结构，
     * 同级节点按 sortOrder 升序排列。
     * </p>
     *
     * @return 菜单树（顶级节点列表）
     */
    public List<SysMenu> getMenuTree() {
        List<SysMenu> all = list();
        Map<Long, List<SysMenu>> childrenMap = all.stream()
                .filter(m -> m.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        return all.stream()
                .filter(m -> m.getParentId() == 0)
                .peek(m -> buildTree(m, childrenMap))
                .sorted(Comparator.comparing(SysMenu::getSortOrder))
                .collect(Collectors.toList());
    }

    /**
     * 递归构建菜单子树。
     *
     * @param parent      父级菜单节点
     * @param childrenMap 按 parentId 分组的子菜单 Map
     */
    private void buildTree(SysMenu parent, Map<Long, List<SysMenu>> childrenMap) {
        List<SysMenu> children = childrenMap.getOrDefault(parent.getId(), new ArrayList<>());
        children.sort(Comparator.comparing(SysMenu::getSortOrder));
        children.forEach(c -> buildTree(c, childrenMap));
        parent.setChildren(children);
    }

    /**
     * 查询指定用户有权限访问的菜单树。
     * <p>
     * 仅返回状态为启用（status=1）且类型为目录或菜单（menuType in 0,1）的菜单项，
     * 按 sortOrder 排序后构建树形结构，用于前端动态路由生成。
     * </p>
     *
     * @param userId 用户 ID
     * @return 用户可见的菜单树
     */
    public List<SysMenu> getUserMenus(Long userId) {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId);
        Map<Long, List<SysMenu>> childrenMap = menus.stream()
                .filter(m -> m.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        return menus.stream()
                .filter(m -> m.getParentId() == 0)
                .peek(m -> buildTree(m, childrenMap))
                .sorted(Comparator.comparing(SysMenu::getSortOrder))
                .collect(Collectors.toList());
    }

    private void collectDescendants(Long parentId, List<SysMenu> all, LinkedHashSet<Long> ids) {
        if (!ids.add(parentId)) return;
        all.stream().filter(menu -> Objects.equals(menu.getParentId(), parentId))
                .forEach(menu -> collectDescendants(menu.getId(), all, ids));
    }

    private void assertNoCycle(Long menuId, Long parentId) {
        Long current = parentId;
        LinkedHashSet<Long> visited = new LinkedHashSet<>();
        while (current != null && current > 0 && visited.add(current)) {
            if (Objects.equals(current, menuId)) throw new IllegalArgumentException("父级菜单不能是当前菜单或其子菜单");
            SysMenu parent = getById(current);
            if (parent == null) throw new IllegalArgumentException("父级菜单不存在");
            current = parent.getParentId();
        }
    }

    private void validateMenu(SysMenu menu) {
        if (menu == null) throw new IllegalArgumentException("菜单信息不能为空");
        String name = menu.getMenuName() == null ? "" : menu.getMenuName().trim();
        if (name.isEmpty() || name.length() > 50) throw new IllegalArgumentException("菜单名称长度须为 1–50 个字符");
        if (menu.getParentId() == null) menu.setParentId(0L);
        if (menu.getParentId() < 0) throw new IllegalArgumentException("父级菜单无效");
        if (menu.getParentId() > 0 && getById(menu.getParentId()) == null) throw new IllegalArgumentException("父级菜单不存在");
        if (menu.getMenuType() == null || menu.getMenuType() < 0 || menu.getMenuType() > 2) {
            throw new IllegalArgumentException("菜单类型无效");
        }
        if (menu.getPerms() != null && menu.getPerms().length() > 100) throw new IllegalArgumentException("权限标识过长");
        if (menu.getPath() != null && menu.getPath().length() > 200) throw new IllegalArgumentException("路由路径过长");
        if (menu.getComponent() != null && menu.getComponent().length() > 200) throw new IllegalArgumentException("组件路径过长");
        if (menu.getSortOrder() == null) menu.setSortOrder(0);
        if (menu.getSortOrder() < 0 || menu.getSortOrder() > 9999) throw new IllegalArgumentException("菜单排序范围为 0–9999");
        if (menu.getStatus() == null) menu.setStatus(1);
        if (menu.getStatus() != 0 && menu.getStatus() != 1) throw new IllegalArgumentException("菜单状态无效");
        menu.setMenuName(name);
    }

    private boolean isCoreSystemMenu(SysMenu menu) {
        return menu != null && ((menu.getPerms() != null && menu.getPerms().startsWith("system:"))
                || (menu.getPath() != null && menu.getPath().startsWith("/system")));
    }
}
