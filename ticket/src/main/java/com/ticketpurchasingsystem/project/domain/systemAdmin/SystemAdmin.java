package com.ticketpurchasingsystem.project.domain.systemAdmin;

public class SystemAdmin {
    private final String id;
    private final AdminInfo adminInfo;

    public SystemAdmin(String id, AdminInfo adminInfo) {
        this.id = id;
        this.adminInfo = adminInfo;
    }

    public String getId() {
        return id;
    }

    public AdminInfo getAdminInfo() {
        return adminInfo;
    }
}
