package com.ticketpurchasingsystem.project.domain.Utils;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class ManagerDTO {
    private final String userId;
    private final String appointerId;
    private final Set<ManagerPermission> permissions;

    public ManagerDTO(String userId, String appointerId, Set<ManagerPermission> permissions) {
        this.userId = userId;
        this.appointerId = appointerId;
        this.permissions = permissions.isEmpty()
                ? Collections.emptySet()
                : EnumSet.copyOf(permissions);
    }

    public String getUserId() {
        return userId;
    }

    public String getAppointerId() {
        return appointerId;
    }

    public Set<ManagerPermission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public boolean hasPermission(ManagerPermission permission) {
        return permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "ManagerDTO{userId='" + userId + "', appointerId='" + appointerId
                + "', permissions=" + permissions + "}";
    }
}
