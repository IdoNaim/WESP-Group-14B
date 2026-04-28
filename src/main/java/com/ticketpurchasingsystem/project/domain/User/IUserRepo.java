package com.ticketpurchasingsystem.project.domain.User;

import java.util.List;

public interface IUserRepo {
    List<UserInfo> findAll();
}
