package com.cyberflow.admin.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyberflow.admin.system.entity.SysRole;
import com.cyberflow.admin.system.entity.SysRoleMenu;
import com.cyberflow.admin.system.mapper.SysRoleMapper;
import com.cyberflow.admin.system.mapper.SysRoleMenuMapper;
import com.cyberflow.admin.system.mapper.SysMenuMapper;
import com.cyberflow.admin.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * 系统角色业务服务。
 * <p>
 * 继承 MyBatis-Plus 的 {@link ServiceImpl}，提供角色的 CRUD 操作。
 * 支持角色与菜单权限的关联管理。
 * </p>
 *
 * @author CyberFlow Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    /** 角色-菜单关联 Mapper，用于菜单权限分配操作 */
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;

    public boolean createRole(SysRole role) {
        validateRole(role);
        if (lambdaQuery().eq(SysRole::getRoleName, role.getRoleName()).count() > 0
                || lambdaQuery().eq(SysRole::getRoleCode, role.getRoleCode()).count() > 0) {
            throw new IllegalArgumentException("角色名称或角色编码已存在");
        }
        return save(role);
    }

    public boolean updateRole(SysRole role) {
        if (role == null || role.getId() == null) throw new IllegalArgumentException("角色不存在");
        SysRole previous = getById(role.getId());
        if (previous == null) throw new IllegalArgumentException("角色不存在");
        role.setRoleCode(previous.getRoleCode());
        validateRole(role);
        if ("ROLE_ADMIN".equals(previous.getRoleCode()) && role.getStatus() == 0) {
            throw new IllegalArgumentException("系统管理员角色不能禁用");
        }
        return updateById(role);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        SysRole role = getById(roleId);
        if (role == null) throw new IllegalArgumentException("角色不存在");
        if ("ROLE_ADMIN".equals(role.getRoleCode())) {
            throw new IllegalArgumentException("系统管理员角色不能删除");
        }
        userRoleMapper.deleteByRoleId(roleId);
        roleMenuMapper.deleteByRoleId(roleId);
        removeById(roleId);
    }

    /**
     * 为角色分配菜单权限。
     * <p>
     * 先删除角色原有的所有菜单权限关联，再批量插入新的关联。
     * 整个过程在同一个事务中执行。
     * </p>
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表，可为空或 null（清空所有权限）
     */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = getById(roleId);
        if (role == null) throw new IllegalArgumentException("角色不存在");
        if ("ROLE_ADMIN".equals(role.getRoleCode())) {
            throw new IllegalArgumentException("系统管理员角色的核心权限不能调整");
        }
        LinkedHashSet<Long> expanded = new LinkedHashSet<>();
        if (menuIds != null) {
            menuIds.stream().filter(Objects::nonNull).forEach(menuId -> addMenuAndParents(menuId, expanded));
        }
        roleMenuMapper.deleteByRoleId(roleId);
        expanded.forEach(menuId -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        });
    }

    private void addMenuAndParents(Long menuId, LinkedHashSet<Long> result) {
        Long currentId = menuId;
        LinkedHashSet<Long> visited = new LinkedHashSet<>();
        while (currentId != null && currentId > 0 && visited.add(currentId)) {
            var menu = menuMapper.selectById(currentId);
            if (menu == null) throw new IllegalArgumentException("菜单不存在或已删除");
            result.add(menu.getId());
            currentId = menu.getParentId();
        }
    }

    private void validateRole(SysRole role) {
        if (role == null) throw new IllegalArgumentException("角色信息不能为空");
        String name = role.getRoleName() == null ? "" : role.getRoleName().trim();
        String code = role.getRoleCode() == null ? "" : role.getRoleCode().trim().toUpperCase();
        if (name.isEmpty() || name.length() > 50) throw new IllegalArgumentException("角色名称长度须为 1–50 个字符");
        if (!code.matches("ROLE_[A-Z0-9_]{1,45}")) throw new IllegalArgumentException("角色编码格式不正确");
        if (role.getDescription() != null && role.getDescription().length() > 200) {
            throw new IllegalArgumentException("角色描述不能超过 200 个字符");
        }
        if (role.getStatus() == null) role.setStatus(1);
        if (role.getStatus() != 0 && role.getStatus() != 1) throw new IllegalArgumentException("角色状态无效");
        role.setRoleName(name);
        role.setRoleCode(code);
    }
}
