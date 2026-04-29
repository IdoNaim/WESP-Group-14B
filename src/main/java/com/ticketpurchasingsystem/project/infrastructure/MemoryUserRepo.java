package com.ticketpurchasingsystem.project.infrastructure;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class MemoryUserRepo implements IUserRepo {
    private final ConcurrentHashMap<String, UserInfo> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SessionToken> guests = new ConcurrentHashMap<>();

    @Override
    public void store(UserInfo userInfo) {
        users.put(userInfo.getId(), userInfo);
    }

    @Override
    public void delete(String userId) {
        users.remove(userId);
    }

    @Override
    public UserInfo findByID(String userId) {
        return users.get(userId);
    }

    @Override
    public List<UserInfo> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public List<SessionToken> getAllGuests() {
        return new ArrayList<>(guests.values());
    }
}
