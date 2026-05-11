package com.ticketpurchasingsystem.project.domain.Production.ProductionEvents;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;

import java.util.Collections;
import java.util.Set;

public class AppointManagerEvent {
    private final ProductionCompany company;
    private final String appointerId;
    private final String managerId;
    private final Set<ManagerPermission> permissions;

    public AppointManagerEvent(ProductionCompany company, String appointerId,
            String managerId, Set<ManagerPermission> permissions) {
        this.company = company;
        this.appointerId = appointerId;
        this.managerId = managerId;
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public ProductionCompany getCompany() {
        return company;
    }

    public String getAppointerId() {
        return appointerId;
    }

    public String getManagerId() {
        return managerId;
    }

    public Set<ManagerPermission> getPermissions() {
        return permissions;
    }
}
