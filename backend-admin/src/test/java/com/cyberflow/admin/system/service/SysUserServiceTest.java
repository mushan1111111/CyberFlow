package com.cyberflow.admin.system.service;

import com.cyberflow.admin.system.entity.SysUser;
import com.cyberflow.admin.system.mapper.SysRoleMapper;
import com.cyberflow.admin.system.mapper.SysUserMapper;
import com.cyberflow.admin.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysUserServiceTest {
    private SysUserMapper userMapper;
    private SysUserRoleMapper userRoleMapper;
    private SysUserService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        service = spy(new SysUserService(userMapper, userRoleMapper, mock(SysRoleMapper.class), passwordEncoder));
    }

    @Test
    void rejectsWeakInitialPasswords() {
        SysUser user = user(1L, "new-user", 1);
        user.setPassword("short");
        assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
    }

    @Test
    void updateCannotChangeTheLoginName() {
        SysUser previous = user(7L, "original", 1);
        SysUser update = user(7L, "hijacked", 1);
        doReturn(previous).when(service).getById(7L);
        doReturn(true).when(service).updateById(update);

        service.updateUser(update);

        assertEquals("original", update.getUsername());
        verify(service).updateById(update);
    }

    @Test
    void lastActiveAdministratorCannotBeDisabled() {
        SysUser previous = user(1L, "admin", 1);
        SysUser update = user(1L, "admin", 0);
        doReturn(previous).when(service).getById(1L);
        when(userMapper.countUserRoleCode(1L, "ROLE_ADMIN")).thenReturn(1L);
        when(userMapper.countActiveUsersByRoleCode("ROLE_ADMIN")).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> service.updateUser(update));
        verify(service, never()).updateById(any());
    }

    @Test
    void deletingAUserAlsoRemovesRoleLinks() {
        SysUser user = user(9L, "former-user", 0);
        doReturn(user).when(service).getById(9L);
        doReturn(true).when(service).removeById(9L);

        service.deleteUser(9L, "admin");

        verify(userRoleMapper).deleteByUserId(9L);
        verify(service).removeById(9L);
    }

    @Test
    void currentAccountCannotDeleteItself() {
        doReturn(user(1L, "admin", 1)).when(service).getById(1L);
        assertThrows(IllegalArgumentException.class, () -> service.deleteUser(1L, "admin"));
        verify(userRoleMapper, never()).deleteByUserId(any());
    }

    @Test
    void sharedOwnersAndFieldsAreNormalizedBeforeSaving() {
        SysUser user = user(11L, "member", 1);
        user.setPassword("password123");
        user.setDataOwner(" A-本人 ");
        user.setSharedDataOwners("A-本人、A-成员,A-成员");
        user.setSharedDataFields("order.amount,performance.site_count,order.amount");
        doReturn(null).when(service).getByUsername("member");
        doReturn(true).when(service).save(user);

        service.createUser(user);

        assertEquals("A-本人", user.getDataOwner());
        assertEquals("A-成员", user.getSharedDataOwners());
        assertEquals("order.amount,performance.site_count", user.getSharedDataFields());
    }

    @Test
    void unknownSharedFieldIsRejected() {
        SysUser user = user(12L, "member", 1);
        user.setPassword("password123");
        user.setSharedDataFields("order.not_a_field");

        assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
    }

    private SysUser user(Long id, String username, int status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(status);
        return user;
    }
}
