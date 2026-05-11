package com.ticketpurchasingsystem.project.application.UserService;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserHandler;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Service
public class UserService implements IUserService {

    private IUserRepo userRepo;    
    private UserHandler userHandler;
    private final AuthenticationService authenticationService;

    public UserService(IUserRepo userRepo, UserHandler userHandler, AuthenticationService authenticationService) {
        this.userRepo = userRepo;
        this.userHandler = userHandler;
        this.authenticationService = authenticationService;
    }

    public void guestEntry() {
        try {
            String sessionToken = authenticationService.login(userHandler.generateUniqueId());
            userRepo.store(userHandler.handleGuestEntry(sessionToken, authenticationService.getUser(sessionToken)));
        } catch (Exception e) {
            // Handle exception
            loggerDef.getInstance().error("Failed to handle guest entry: " + e.getMessage());
        }
    };

    public void Exit(String sessionTokenStr) {
        try {
            if (authenticationService.validate(sessionTokenStr)) {
                authenticationService.logout(sessionTokenStr);
                userHandler.handleExit(userRepo.findByID(authenticationService.getUser((sessionTokenStr))));
            }
        } catch (Exception e) {
            // Handle exception
            loggerDef.getInstance().error("Failed to handle exit: " + e.getMessage());
        }
    };

    // register the user without logging in
    public void registerUser(String userId, String name, String password, String email, UserGroupDiscount userGroupDiscount, String sessionTokenStr) {
        userHandler.registerUser(userId, name, email, password, userGroupDiscount, userRepo, sessionTokenStr);
    }; 

    public String loginUser(String userId, String password, String sessionTokenStr) {
        return userHandler.loginUser(userRepo, userId, password, sessionTokenStr);
    };
    public void logoutUser(String userId, String sessionTokenStr) {
        userHandler.logoutUser(userRepo, userId, sessionTokenStr);
    };

    public List<UserDTO> getAllUsers() {
        return userHandler.getAllUsers(userRepo);
    }; 

    public UserDTO getUser(String userId) {
        return userHandler.getUser(userRepo, userId);
    };

    public void deleteUser(String userId, String sessionTokenStr) {
        userHandler.deleteUser(userRepo, userId, sessionTokenStr);
    };

    public void editUsername(String userId, String oldUsername, String newUsername, String sessionTokenStr) {
        userHandler.editUsername(userRepo, userId, oldUsername, newUsername, sessionTokenStr);
    };

    public void editPassword(String userId, String oldPassword, String newPassword, String sessionTokenStr) {
        userHandler.editPassword(userRepo, userId, oldPassword, newPassword, sessionTokenStr);
    };

    public void editEmail(String userId, String oldEmail, String newEmail, String sessionTokenStr) {
        userHandler.editEmail(userRepo, userId, oldEmail, newEmail, sessionTokenStr);
    };

    public void setUserGroupDiscount(String userId, UserGroupDiscount userGroupDiscount, String sessionTokenStr) {
        userHandler.setUserGroupDiscount(userRepo, userId, userGroupDiscount, sessionTokenStr);
    };

}
