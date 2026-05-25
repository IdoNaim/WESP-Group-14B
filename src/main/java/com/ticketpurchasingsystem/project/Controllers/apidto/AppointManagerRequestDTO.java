package com.ticketpurchasingsystem.project.Controllers.apidto;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;

import java.util.Set;

public class AppointManagerRequestDTO {
    private String managerId;
    private Set<ManagerPermission> permissions;

    public AppointManagerRequestDTO() {}

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public Set<ManagerPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<ManagerPermission> permissions) {
        this.permissions = permissions;
    }
}
