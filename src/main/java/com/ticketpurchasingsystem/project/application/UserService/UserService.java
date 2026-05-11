package com.ticketpurchasingsystem.project.application.UserService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserHandler;
import com.ticketpurchasingsystem.project.domain.User.UserProduction;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Service
public class UserService implements IUserService {

    private final IUserRepo userRepo;    
    private final UserHandler userHandler;
    private final AuthenticationService authenticationService;
    private final UserPublisher userPublisher;

    public UserService(IUserRepo userRepo, UserHandler userHandler, AuthenticationService authenticationService, UserPublisher userPublisher) {
        this.userRepo = userRepo;
        this.userHandler = userHandler;
        this.authenticationService = authenticationService;
        this.userPublisher = userPublisher;
    }

    public void guestEntry() {
        try {
            String sessionToken = authenticationService.login(userHandler.generateUniqueId());
            UserInfo guest = userHandler.handleGuestEntry(sessionToken, authenticationService.getUser(sessionToken));
            userRepo.store(guest);
            userPublisher.publishGuestEntered(guest.getId(), sessionToken);
            loggerDef.getInstance().info("Guest entry successful. Guest ID: " + guest.getId());
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to handle guest entry: " + e.getMessage());
        }
    }

    public void Exit(String sessionTokenStr) {
        try {
            if (authenticationService.validate(sessionTokenStr)) {
                String userId = authenticationService.getUser(sessionTokenStr);
                UserInfo userInfo = userRepo.findByID(userId);
                
                if (userInfo != null) {
                    if (userInfo.isGuest()) {
                        userRepo.delete(userInfo.getId());
                        userPublisher.publishGuestExited(userInfo.getId(), userInfo.getSessionTokenStr());
                    } else {
                        userHandler.handleUserExit(userInfo);
                        userRepo.store(userInfo);
                        userPublisher.publishUserLeftPlatform(userInfo.getId(), sessionTokenStr);
                    }
                }
                authenticationService.logout(sessionTokenStr);
                loggerDef.getInstance().info("Exit successful for token/user.");
            }
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to handle exit: " + e.getMessage());
        }
    }

    public void registerUser(String userId, String name, String password, String email, UserGroupDiscount userGroupDiscount, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            if (userRepo.findByID(userId) != null) {
                throw new RuntimeException("User with the same ID already exists.");
            }
            UserInfo newUser = userHandler.registerUser(userId, name, email, password, userGroupDiscount);
            userRepo.store(newUser);
            userPublisher.publishUserCreated(userId);
            loggerDef.getInstance().info("User registered successfully: " + userId);
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to register user: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String loginUser(String userId, String password, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            UserInfo userInfo = userRepo.findByID(userId);
            
            // Generate a fresh session token via auth service
            String newSessionTokenStr = authenticationService.login(userId);

            // Let handler update login status and state
            userHandler.loginUser(userInfo, password, newSessionTokenStr);
            
            // Delete guest matching the OLD session token before saving the user
            String guestId = authenticationService.getUser(sessionTokenStr);
            if (guestId != null && userRepo.findByID(guestId) != null && userRepo.findByID(guestId).isGuest()) {
                userRepo.delete(guestId);
                userPublisher.publishGuestExited(guestId, sessionTokenStr);
            }
            authenticationService.logout(sessionTokenStr);

            userRepo.store(userInfo);
            userPublisher.publishUserLoggedIn(userId, newSessionTokenStr);
            loggerDef.getInstance().info("User logged in successfully: " + userId);
            return newSessionTokenStr;
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to log in user: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void logoutUser(String userId, String sessionToken) {
        try {
            if (!authenticationService.validate(sessionToken)) {
                throw new RuntimeException("Invalid session token.");
            }
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            userHandler.logoutUser(userInfo);
            userRepo.store(userInfo);
            userPublisher.publishUserLoggedOut(userId, sessionToken);

            authenticationService.logout(sessionToken);
            String newGuestSession = authenticationService.login(userHandler.generateUniqueId());
            UserInfo guest = userHandler.handleGuestEntry(newGuestSession, authenticationService.getUser(newGuestSession));
            userRepo.store(guest);
            userPublisher.publishGuestEntered(guest.getId(), newGuestSession);
            loggerDef.getInstance().info("User logged out successfully: " + userId);
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to log out user: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<UserDTO> getAllUsers() {
        return userRepo.getAllUsers().stream()
                .map(userHandler::mapToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUser(String userId) {
        UserInfo userInfo = userRepo.findByID(userId);
        if (userInfo == null) {
            throw new RuntimeException("User not found.");
        }
        return userHandler.mapToDTO(userInfo);
    }

    public void deleteUser(String userId, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            UserInfo userInfo = userRepo.findByID(userId);
            userHandler.validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
            
            userHandler.logoutUser(userInfo);
            userRepo.delete(userId);
            // User deleted
            loggerDef.getInstance().info("User deleted successfully: " + userId);
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to delete user: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void editUsername(String userId, String oldUsername, String newUsername, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            UserInfo userInfo = userRepo.findByID(userId);
            userHandler.editUsername(userInfo, userId, oldUsername, newUsername, sessionTokenStr);
            userRepo.store(userInfo);
            loggerDef.getInstance().info("Username edited successfully for user: " + userId);
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to edit username: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void editPassword(String userId, String oldPassword, String newPassword, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            UserInfo userInfo = userRepo.findByID(userId);
            userHandler.editPassword(userInfo, userId, oldPassword, newPassword, sessionTokenStr);
            userRepo.store(userInfo);
            loggerDef.getInstance().info("Password edited successfully for user: " + userId);
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to edit password: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void editEmail(String userId, String oldEmail, String newEmail, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            UserInfo userInfo = userRepo.findByID(userId);
            userHandler.editEmail(userInfo, userId, oldEmail, newEmail, sessionTokenStr);
            userRepo.store(userInfo);
            loggerDef.getInstance().info("Email edited successfully for user: " + userId);
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to edit email: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void setUserGroupDiscount(String userId, UserGroupDiscount userGroupDiscount, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            UserInfo userInfo = userRepo.findByID(userId);
            userHandler.setUserGroupDiscount(userInfo, userId, userGroupDiscount, sessionTokenStr);
            userRepo.store(userInfo);
            loggerDef.getInstance().info("User group discount set successfully for user: " + userId);
        } catch (Exception e) {
             loggerDef.getInstance().error("Failed to set user group discount: " + e.getMessage());
             throw new RuntimeException(e);
        }
    }
    
    public void assignProductionRole(String userId, Integer companyId, UserProduction.RoleInProduction role) {
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) throw new RuntimeException("User not found.");
            userHandler.addProductionRole(userInfo, companyId, role);
            userRepo.store(userInfo);
            loggerDef.getInstance().info("Production role " + role + " assigned successfully to user: " + userId + " for company ID: " + companyId);
        } catch (Exception e) {
             loggerDef.getInstance().error("Failed to assign production role: " + e.getMessage());
        }
    }
    public boolean isUserRegistered(String userId){
        UserInfo user = userRepo.findByID(userId);
        return user != null;
    }

}
