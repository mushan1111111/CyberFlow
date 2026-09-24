package com.cyberflow.admin.common;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Effective row-level scope for the current operator.
 *
 * <p>Administrators receive an unrestricted scope. Other modules use the
 * account's own external identity; explicitly supported modules may also use
 * shared owners and then enforce the shared-field whitelist.</p>
 */
public record DataScope(boolean administrator, boolean operator, String ownerName,
                        List<String> sharedOwnerNames, Set<String> sharedFields) {
    public DataScope {
        sharedOwnerNames = sharedOwnerNames == null ? List.of() : List.copyOf(sharedOwnerNames);
        sharedFields = sharedFields == null ? Set.of() : Set.copyOf(sharedFields);
    }

    public static DataScope all() {
        return new DataScope(true, false, null, List.of(), Set.of());
    }

    /** Other modules remain restricted to the user's own external account. */
    public List<String> ownerNames() {
        return ownerName == null || ownerName.isBlank() || "\u0000".equals(ownerName)
                ? List.of() : List.of(ownerName);
    }

    public boolean owns(String value) {
        return ownerName != null && ownerName.equals(value);
    }

    public boolean canViewSharedField(String field) {
        return administrator || sharedFields.contains(field);
    }

    public boolean hasSharedFields(String prefix) {
        return administrator || sharedFields.stream().anyMatch(field -> field.startsWith(prefix));
    }

    public List<String> ownerNamesFor(String prefix) {
        if (administrator) return List.of();
        var owners = new LinkedHashSet<>(ownerNames());
        if (hasSharedFields(prefix)) owners.addAll(sharedOwnerNames);
        return List.copyOf(owners);
    }

    public String ownerFilterFor(String prefix) {
        if (administrator) return null;
        List<String> owners = ownerNamesFor(prefix);
        return owners.isEmpty() ? "\u0000" : String.join(",", owners);
    }

    public String ownOwnerFilter() {
        return ownerNames().isEmpty() ? "\u0000" : ownerName;
    }

    public boolean includesSharedOwners(String prefix) {
        return !administrator && hasSharedFields(prefix) && !sharedOwnerNames.isEmpty();
    }
}
