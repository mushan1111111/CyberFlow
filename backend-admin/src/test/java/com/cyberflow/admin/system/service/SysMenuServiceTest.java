package com.cyberflow.admin.system.service;

import com.cyberflow.admin.system.entity.SysMenu;
import com.cyberflow.admin.system.mapper.SysMenuMapper;
import com.cyberflow.admin.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class SysMenuServiceTest {
    private SysRoleMenuMapper roleMenuMapper;
    private SysMenuService service;

    @BeforeEach
    void setUp() {
        roleMenuMapper = mock(SysRoleMenuMapper.class);
        service = spy(new SysMenuService(mock(SysMenuMapper.class), roleMenuMapper));
    }

    @Test
    void deletingAParentCleansDescendantsAndRoleLinks() {
        SysMenu root = menu(3L, 0L);
        SysMenu child = menu(31L, 3L);
        doReturn(root).when(service).getById(3L);
        doReturn(List.of(root, child)).when(service).list();
        doReturn(true).when(service).removeByIds(anyCollection());

        service.deleteMenu(3L);

        verify(roleMenuMapper).deleteByMenuId(3L);
        verify(roleMenuMapper).deleteByMenuId(31L);
        verify(service).removeByIds(anyCollection());
    }

    @Test
    void menuCannotMoveBelowItsOwnDescendant() {
        SysMenu root = menu(3L, 0L);
        root.setMenuName("系统管理");
        root.setMenuType(0);
        root.setSortOrder(1);
        root.setStatus(1);
        root.setParentId(31L);
        SysMenu child = menu(31L, 3L);
        doAnswer(invocation -> Long.valueOf(31L).equals(invocation.getArgument(0)) ? child : root)
                .when(service).getById(anyLong());

        assertThrows(IllegalArgumentException.class, () -> service.updateMenu(root));
        verify(service, never()).updateById(any());
    }

    @Test
    void coreSystemPermissionsCannotBeDeleted() {
        SysMenu systemPermission = menu(35L, 31L);
        systemPermission.setPerms("system:user:create");
        doReturn(systemPermission).when(service).getById(35L);
        doReturn(List.of(systemPermission)).when(service).list();

        assertThrows(IllegalArgumentException.class, () -> service.deleteMenu(35L));
        verifyNoInteractions(roleMenuMapper);
    }

    private SysMenu menu(Long id, Long parentId) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        return menu;
    }
}
