package com.ticketpurchasingsystem.project.infrastructure;

import org.springframework.stereotype.Repository;

import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MemoryUserRepo implements IUserRepo {
    private final ConcurrentHashMap<String, UserInfo> users = new ConcurrentHashMap<>();

    @Override
    public void store(UserInfo userInfo) {
        loggerDef.getInstance().info("Storing user: " + userInfo.getId());
        users.put(userInfo.getId(), userInfo);
    }

    @Override
    public void delete(String userId) {
        users.remove(userId);
    }

    @Override
    public UserInfo findByID(String userId) {
        loggerDef.getInstance().info("Finding user by ID: " + userId);
        return users.get(userId);
    }

    @Override
    public List<UserInfo> getAllUsers() {
        return new ArrayList<>(users.values());
    }
}
