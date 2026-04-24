package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import java.util.List;

public interface ISystemAdminService {
    List<UserInfo> getAllUsers();
}
