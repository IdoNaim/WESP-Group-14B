package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.List;

public interface IAdminRepo {
    void save(AdminInfo adminInfo);
    AdminInfo findById(String adminId);
    boolean isAdmin(String adminId);
    List<AdminInfo> findAll();
    boolean isAdminByUserId(String userId);
}
