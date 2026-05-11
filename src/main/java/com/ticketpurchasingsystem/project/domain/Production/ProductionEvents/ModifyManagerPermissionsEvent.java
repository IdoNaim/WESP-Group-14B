package com.ticketpurchasingsystem.project.domain.Production.ProductionEvents;

import java.util.Set;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;

public class ModifyManagerPermissionsEvent {
    private final ProductionCompany company;
    private final String ownerId;
    private final String managerId;
    private final Set<ManagerPermission> permissions;

    public ModifyManagerPermissionsEvent(ProductionCompany company, String ownerId,
            String managerId, Set<ManagerPermission> permissions) {
        this.company = company;
        this.ownerId = ownerId;
        this.managerId = managerId;
        this.permissions = permissions;
    }

    public ProductionCompany getCompany() {
        return company;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getManagerId() {
        return managerId;
    }

    public Set<ManagerPermission> getPermissions() {
        return permissions;
    }
}
