package com.cyberflow.admin.common;

import com.cyberflow.admin.system.entity.SysUser;
import com.cyberflow.admin.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Resolves the backend-enforced row scope for the authenticated user. */
@Service
@RequiredArgsConstructor
public class DataScopeService {
    private final SysUserMapper userMapper;

    public DataScope current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("当前用户未认证");
        }

        return forUsername(authentication.getName());
    }

    public DataScope forUsername(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new IllegalStateException("当前用户不存在");
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        if (roles.stream().anyMatch("ROLE_ADMIN"::equalsIgnoreCase)) {
            return DataScope.all();
        }

        String ownerName = normalizeOwner(user.getDataOwner());
        List<String> sharedOwners = normalizeOwners(user.getSharedDataOwners()).stream()
                .filter(owner -> !owner.equals(ownerName))
                .toList();
        Set<String> sharedFields = normalizeFields(user.getSharedDataFields());
        boolean operator = roles.stream().anyMatch("ROLE_OPERATOR"::equalsIgnoreCase);
        return new DataScope(false, operator, ownerName, sharedOwners, sharedFields);
    }

    private static String normalizeOwner(String value) {
        String owner = value == null ? "" : value.trim();
        return owner.isBlank() ? "\u0000" : owner;
    }

    private static List<String> normalizeOwners(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("[,，、\\n]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private static Set<String> normalizeFields(String value) {
        if (value == null || value.isBlank()) return Set.of();
        var result = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(SharedDataFields.all()::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return Set.copyOf(result);
    }
}
