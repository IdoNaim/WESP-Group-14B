package com.ticketpurchasingsystem.project.Controllers.apidto;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;

import java.util.Set;

public class ModifyPermissionsRequestDTO {
    private Set<ManagerPermission> permissions;

    public ModifyPermissionsRequestDTO() {}

    public Set<ManagerPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<ManagerPermission> permissions) {
        this.permissions = permissions;
    }
}
