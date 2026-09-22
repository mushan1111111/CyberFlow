package com.cyberflow.admin.system.service;

import com.cyberflow.admin.system.entity.SysMenu;
import com.cyberflow.admin.system.entity.SysRole;
import com.cyberflow.admin.system.entity.SysRoleMenu;
import com.cyberflow.admin.system.mapper.SysMenuMapper;
import com.cyberflow.admin.system.mapper.SysRoleMapper;
import com.cyberflow.admin.system.mapper.SysRoleMenuMapper;
import com.cyberflow.admin.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SysRoleServiceTest {
    private SysRoleMenuMapper roleMenuMapper;
    private SysUserRoleMapper userRoleMapper;
    private SysMenuMapper menuMapper;
    private SysRoleService service;

    @BeforeEach
    void setUp() {
        roleMenuMapper = mock(SysRoleMenuMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        menuMapper = mock(SysMenuMapper.class);
        service = spy(new SysRoleService(roleMenuMapper, userRoleMapper, menuMapper));
    }

    @Test
    void administratorRoleCannotBeDeleted() {
        doReturn(role(1L, "ROLE_ADMIN", 1)).when(service).getById(1L);
        assertThrows(IllegalArgumentException.class, () -> service.deleteRole(1L));
        verifyNoInteractions(userRoleMapper, roleMenuMapper);
    }

    @Test
    void administratorRolePermissionsCannotBeRemoved() {
        doReturn(role(1L, "ROLE_ADMIN", 1)).when(service).getById(1L);
        assertThrows(IllegalArgumentException.class, () -> service.assignMenus(1L, List.of()));
        verifyNoInteractions(roleMenuMapper, menuMapper);
    }

    @Test
    void deletingARoleCleansBothAssociationTables() {
        doReturn(role(2L, "ROLE_OPERATOR", 1)).when(service).getById(2L);
        doReturn(true).when(service).removeById(2L);

        service.deleteRole(2L);

        verify(userRoleMapper).deleteByRoleId(2L);
        verify(roleMenuMapper).deleteByRoleId(2L);
        verify(service).removeById(2L);
    }

    @Test
    void assigningAChildMenuAutomaticallyIncludesItsParents() {
        doReturn(role(2L, "ROLE_OPERATOR", 1)).when(service).getById(2L);
        when(menuMapper.selectById(31L)).thenReturn(menu(31L, 3L));
        when(menuMapper.selectById(3L)).thenReturn(menu(3L, 0L));

        service.assignMenus(2L, List.of(31L));

        ArgumentCaptor<SysRoleMenu> rows = ArgumentCaptor.forClass(SysRoleMenu.class);
        verify(roleMenuMapper, times(2)).insert(rows.capture());
        assertEquals(List.of(31L, 3L), rows.getAllValues().stream().map(SysRoleMenu::getMenuId).toList());
    }

    private SysRole role(Long id, String code, int status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(code);
        role.setRoleCode(code);
        role.setStatus(status);
        return role;
    }

    private SysMenu menu(Long id, Long parentId) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        return menu;
    }
}
