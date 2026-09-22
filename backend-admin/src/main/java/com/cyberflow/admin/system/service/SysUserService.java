package com.cyberflow.admin.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyberflow.admin.system.entity.SysUser;
import com.cyberflow.admin.system.entity.SysUserRole;
import com.cyberflow.admin.system.mapper.SysRoleMapper;
import com.cyberflow.admin.system.mapper.SysUserMapper;
import com.cyberflow.admin.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * 系统用户业务服务。
 * <p>
 * 继承 MyBatis-Plus 的 {@link ServiceImpl}，提供用户的 CRUD 操作。
 * 同时实现 Spring Security 的 {@link UserDetailsService}，用于认证时加载用户信息。
 * 包含密码加密、角色分配和权限查询等业务逻辑。
 * </p>
 *
 * @author CyberFlow Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> implements UserDetailsService {

    /** 用户 Mapper，用于执行自定义 SQL（角色、权限查询） */
    private final SysUserMapper userMapper;

    /** 用户-角色关联 Mapper，用于角色分配操作 */
    private final SysUserRoleMapper userRoleMapper;

    private final SysRoleMapper roleMapper;

    /** 密码编码器，用于密码加密和校验 */
    private final PasswordEncoder passwordEncoder;

    /**
     * Spring Security 认证时加载用户信息。
     * <p>
     * 根据用户名查询用户，若用户不存在或已被禁用则抛出异常。
     * 查询出用户关联的角色和权限后，构建 Spring Security 的 {@link UserDetails} 对象。
     * </p>
     *
     * @param username 登录用户名
     * @return UserDetails 对象，包含用户名、密码和权限集合
     * @throws UsernameNotFoundException 当用户不存在或已被禁用时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> perms = userMapper.selectPermissionsByUserId(user.getId());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .authorities(perms.toArray(new String[0]))
                .build();
    }

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体，若不存在则返回 null
     */
    public SysUser getByUsername(String username) {
        return lambdaQuery().eq(SysUser::getUsername, username).one();
    }

    /**
     * 创建新用户（密码会在保存前加密）。
     *
     * @param user 新用户实体，密码为明文
     * @return true 表示创建成功
     */
    @Transactional
    public boolean createUser(SysUser user) {
        validateUser(user, true);
        if (getByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return save(user);
    }

    /**
     * 更新用户信息。
     * <p>
     * 若传入的密码非空且非空白，则加密后更新；否则不修改密码字段。
     * </p>
     *
     * @param user 更新后的用户实体
     * @return true 表示更新成功
     */
    @Transactional
    public boolean updateUser(SysUser user) {
        return updateUser(user, null);
    }

    @Transactional
    public boolean updateUser(SysUser user, String currentUsername) {
        if (user == null || user.getId() == null) throw new IllegalArgumentException("用户不存在");
        SysUser previous = getById(user.getId());
        if (previous == null) throw new IllegalArgumentException("用户不存在");
        user.setUsername(previous.getUsername());
        validateUser(user, false);
        if (Objects.equals(previous.getUsername(), currentUsername) && user.getStatus() == 0) {
            throw new IllegalArgumentException("不能禁用当前登录账号");
        }
        if (previous.getStatus() == 1 && user.getStatus() == 0
                && hasRole(previous.getId(), "ROLE_ADMIN")
                && userMapper.countActiveUsersByRoleCode("ROLE_ADMIN") <= 1) {
            throw new IllegalArgumentException("至少需要保留一个启用的管理员账号");
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            validatePassword(user.getPassword());
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        return updateById(user);
    }

    /**
     * 为用户分配角色。
     * <p>
     * 先删除用户原有的所有角色关联，再批量插入新的角色关联。
     * 整个过程在同一个事务中执行。
     * </p>
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表，可为空或 null（清空所有角色）
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        SysUser target = getById(userId);
        if (target == null) throw new IllegalArgumentException("用户不存在");
        List<Long> normalized = roleIds == null ? List.of()
                : new LinkedHashSet<>(roleIds).stream().filter(Objects::nonNull).toList();
        if (!normalized.isEmpty() && roleMapper.selectBatchIds(normalized).size() != normalized.size()) {
            throw new IllegalArgumentException("角色不存在或已删除");
        }
        boolean removingLastAdmin = target.getStatus() == 1 && hasRole(userId, "ROLE_ADMIN")
                && normalized.stream().noneMatch(this::isAdminRole)
                && userMapper.countActiveUsersByRoleCode("ROLE_ADMIN") <= 1;
        if (removingLastAdmin) {
            throw new IllegalArgumentException("至少需要保留一个启用的管理员账号");
        }
        userRoleMapper.deleteByUserId(userId);
        normalized.forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
        });
    }

    @Transactional
    public void deleteUser(Long userId, String currentUsername) {
        SysUser user = getById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (Objects.equals(user.getUsername(), currentUsername)) {
            throw new IllegalArgumentException("不能删除当前登录账号");
        }
        if (user.getStatus() == 1 && hasRole(userId, "ROLE_ADMIN")
                && userMapper.countActiveUsersByRoleCode("ROLE_ADMIN") <= 1) {
            throw new IllegalArgumentException("至少需要保留一个启用的管理员账号");
        }
        userRoleMapper.deleteByUserId(userId);
        removeById(userId);
    }

    private boolean hasRole(Long userId, String roleCode) {
        return userMapper.countUserRoleCode(userId, roleCode) > 0;
    }

    private boolean isAdminRole(Long roleId) {
        var role = roleMapper.selectById(roleId);
        return role != null && "ROLE_ADMIN".equals(role.getRoleCode());
    }

    private void validateUser(SysUser user, boolean requirePassword) {
        if (user == null) throw new IllegalArgumentException("用户信息不能为空");
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        if (username.isEmpty() || username.length() > 50) {
            throw new IllegalArgumentException("用户名长度须为 1–50 个字符");
        }
        user.setUsername(username);
        if (requirePassword) validatePassword(user.getPassword());
        if (user.getNickname() != null && user.getNickname().length() > 50) {
            throw new IllegalArgumentException("昵称不能超过 50 个字符");
        }
        if (user.getDataOwner() != null && user.getDataOwner().length() > 1000) {
            throw new IllegalArgumentException("数据归属内容过长");
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()
                && (!user.getEmail().contains("@") || user.getEmail().length() > 100)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        if (user.getStatus() == null) user.setStatus(1);
        if (user.getStatus() != 0 && user.getStatus() != 1) {
            throw new IllegalArgumentException("用户状态无效");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("密码长度须为 8–72 个字符");
        }
    }
}
