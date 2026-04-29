package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.User.UserInfo;

public class SystemAdmin {

    private final AdminInfo adminInfo;
    private final AdminPublisher adminPublisher;

    public SystemAdmin(AdminInfo adminInfo, AdminPublisher adminPublisher) {
        this.adminInfo = adminInfo;
        this.adminPublisher = adminPublisher;
    }

    public List<UserInfo> getUsersInfo() {
        return adminPublisher.publishGetAllUsers(this.adminInfo.getId());
    }
}
