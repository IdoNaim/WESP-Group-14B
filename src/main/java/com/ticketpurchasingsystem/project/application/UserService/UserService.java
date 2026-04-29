package com.ticketpurchasingsystem.project.application.UserService;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserHandler;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.User.UserListener;
import com.ticketpurchasingsystem.project.domain.User.UserPublisher;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;

@Service
public class UserService implements IUserService {

    private IUserRepo userRepo;    
    private UserHandler userHandler;

    public UserService(IUserRepo userRepo, UserHandler userHandler) {
        this.userRepo = userRepo;
        // this.userPublisher = new UserPublisher();
        this.userHandler = userHandler;
    }

    public String guestEntry() {
        return userHandler.handleGuestEntry(userRepo);
    };

    public void Exit(String sessionTokenStr) {
        userHandler.handleExit(userRepo, sessionTokenStr);
    };

    // register the user without logging in
    public void registerUser(String userId, String name, String password, String email, UserGroupDiscount userGroupDiscount) {
        userHandler.registerUser(userId, name, email, password, userGroupDiscount, userRepo);
    }; 

    public String loginUser(String userId, String password, String sessionTokenStr) {
        return userHandler.loginUser(userRepo, userId, password, sessionTokenStr);
    };
    public void logoutUser(String userId) {
        userHandler.logoutUser(userRepo, userId);
    };

    public List<UserDTO> getAllUsers() {
        return userHandler.getAllUsers(userRepo);
    }; 

    public UserDTO getUser(String userId) {
        return userHandler.getUser(userRepo, userId);
    };

    public void deleteUser(String userId) {
        userHandler.deleteUser(userRepo, userId);
    };

    public void editUsername(String userId, String oldUsername, String newUsername) {
        userHandler.editUsername(userRepo, userId, oldUsername, newUsername);
    };

    public void editPassword(String userId, String oldPassword, String newPassword) {
        userHandler.editPassword(userRepo, userId, oldPassword, newPassword);
    };

    public void editEmail(String userId, String oldEmail, String newEmail) {
        userHandler.editEmail(userRepo, userId, oldEmail, newEmail);
    };

    public void setUserGroupDiscount(String userId, UserGroupDiscount userGroupDiscount) {
        userHandler.setUserGroupDiscount(userRepo, userId, userGroupDiscount);
    };

}
