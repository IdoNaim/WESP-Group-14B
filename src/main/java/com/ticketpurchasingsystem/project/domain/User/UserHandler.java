package com.ticketpurchasingsystem.project.domain.User;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.Utils.PasswordEncoderUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class UserHandler {
    private final AuthenticationService authenticationService;
    private final UserPublisher userPublisher;

    public UserHandler( AuthenticationService authenticationService, UserPublisher userPublisher) {
        this.authenticationService = authenticationService;
        this.userPublisher = userPublisher;
    }

    public boolean isUserLoggedIn(IUserRepo userRepo, String userId) {
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                return false;
            }
            return userInfo.isLoggedIn();
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found)
            throw new RuntimeException("Failed to check user login status: " + e.getMessage());
        }
    }

    public void registerUser(String userId, String name, String email, String password, UserGroupDiscount userGroupDiscount,IUserRepo userRepo, String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr)) {
            throw new RuntimeException("Invalid session token.");
        }
        String encryptedPass = PasswordEncoderUtil.encodePassword(password);
        UserInfo userInfo = new UserInfo(userId, name, email, encryptedPass, userGroupDiscount);
        try {
            if (userRepo.findByID(userId) != null) {
                throw new RuntimeException("User with the same ID already exists.");
            }
            userRepo.store(userInfo);
            userPublisher.publishUserCreated(userId);
        } catch (Exception e) {
            // Handle exceptions (e.g., user already exists)
            throw new RuntimeException("Failed to register user: " + e.getMessage());
        }
    }

    public UserInfo handleGuestEntry(String SessionTokenStr, String guestId) {
        UserInfo guestInfo = new UserInfo(guestId, SessionTokenStr);
        return guestInfo;
    }

    public void handleExit(UserInfo userInfo) {
        try {
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            if (userInfo.isGuest()) {
                handleGuestExit(userRepo, userInfo);
            }
            else {
                handleUserExit(userRepo, userInfo);
            }
        } catch (Exception e) {
            // Handle exceptions (e.g., user not found)
            throw new RuntimeException("Failed to handle exit: " + e.getMessage());
        }
    }

    public void handleGuestExit(IUserRepo userRepo, UserInfo guestUser) {
        try {
            userRepo.delete(guestUser.getId());
            userPublisher.publishGuestExited(guestUser.getId(), guestUser.getSessionTokenStr());
        } catch (Exception e) {
            // Handle exceptions (e.g., failed to delete guest user)
            throw new RuntimeException("Failed to handle guest exit: " + e.getMessage());
        }
    }

    public void handleUserExit(IUserRepo userRepo, UserInfo userInfo) {
        try {
            userInfo.LoggedIn = false;
            String sessionTokenStr = userInfo.getSessionTokenStr(); // Store the session token before clearing it
            userInfo.setSessionTokenStr(null); // Clear the session token on exit
            userRepo.store(userInfo); // Update the user info in the repository
            userPublisher.publishUserLeftPlatform(userInfo.getId(), sessionTokenStr);
        } catch (Exception e) {
            // Handle exceptions (e.g., failed to update user info)
            throw new RuntimeException("Failed to handle user exit: " + e.getMessage());
        }
    }


    public void deleteGuestBeforeLogin(IUserRepo userRepo, String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr)) {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            String guestId = authenticationService.getUser(sessionTokenStr);
            userRepo.delete(guestId);
            authenticationService.logout(sessionTokenStr); // Invalidate the session token
            userPublisher.publishGuestExited(guestId, sessionTokenStr);
        } catch (Exception e) {
            // Handle exceptions (e.g., guest user not found)
            throw new RuntimeException("Failed to delete guest user before login: " + e.getMessage());
        }
    }

    public String setNewSessionToken(UserInfo userInfo) {
        String newSessionToken = authenticationService.login(userInfo.getId());
        userInfo.setSessionTokenStr(newSessionToken);
        return newSessionToken;
    }

    public String loginUser(IUserRepo userRepo, String userId, String password, String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr))
        {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null || !PasswordEncoderUtil.matches(password, userInfo.getPassword())) {
                throw new RuntimeException("Invalid user ID or password.");
            }
            if (userInfo.isLoggedIn()) {
                throw new RuntimeException("User is already logged in.");
            }
            deleteGuestBeforeLogin(userRepo, sessionTokenStr);

            String userSessionTokenStr = setNewSessionToken(userInfo);
            userInfo.LoggedIn = true;
            userPublisher.publishUserLoggedIn(userId, userSessionTokenStr);
            userRepo.store(userInfo); // Update the user info in the repository
            return userSessionTokenStr;
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, incorrect password)
            throw new RuntimeException("Failed to log in user: " + e.getMessage());
        }
    }


    public String logoutUser(IUserRepo userRepo, String userId, String sessionToken) {
        if (!authenticationService.validate(sessionToken)) {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null || !userInfo.isLoggedIn()) {
                throw new RuntimeException("User is not found.");
            }
            userInfo.LoggedIn = false;
            String sessionTokenStr = userInfo.getSessionTokenStr(); // Store the session token before clearing it
            userInfo.setSessionTokenStr(null); // Clear the session token on logout
            userRepo.store(userInfo); // Update the user info in the repository
            userPublisher.publishUserLoggedOut(userId, sessionTokenStr);
            return handleGuestEntry(userRepo); // Create a new guest entry for the user after logging out
        } catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to log out user: " + e.getMessage());
        }
    }


    public UserDTO getUser(IUserRepo userRepo, String userId)
    {
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            ObjectMapper objectMapper = new ObjectMapper();
            UserDTO userDTO = objectMapper.convertValue(userInfo, UserDTO.class);
            return userDTO;
        } catch (Exception e) {
            // Handle exceptions (e.g., user not found)
            throw new RuntimeException("Failed to get user info: " + e.getMessage());
        }
    }

    public String generateUniqueId() {
        return java.util.UUID.randomUUID().toString();
    }

    public List<UserDTO> getAllUsers(IUserRepo userRepo) {
        try {
            List<UserInfo> userInfos = userRepo.getAllUsers();
            ObjectMapper objectMapper = new ObjectMapper();
            List<UserDTO> userDTOs = userInfos.stream()
                    .map(userInfo -> objectMapper.convertValue(userInfo, UserDTO.class))
                    .toList();
            return userDTOs;
        } catch (Exception e) {
            // Handle exceptions (e.g., failed to retrieve users)
            throw new RuntimeException("Failed to get all users: " + e.getMessage());
        }
    }

    public Boolean validateUserEditingHisAccount(UserInfo userInfo, String userId, String sessionTokenStr) {
        return userInfo.getSessionTokenStr() != null && userInfo.getSessionTokenStr().equals(sessionTokenStr) && userInfo.getId().equals(userId);
    }

    public void deleteUser(IUserRepo userRepo, String userId, String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr)) {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            if (validateUserEditingHisAccount(userInfo, userId, sessionTokenStr)) { // only the user can delete his own account
                    logoutUser(userRepo, userId, sessionTokenStr); // Log out the user before deletion
                    userRepo.delete(userId);
                }
            else {
                throw new RuntimeException("Users can only delete their own accounts.");
            }
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }

    public void editUsername(IUserRepo userRepo, String userId, String oldUsername, String newUsername,
            String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr)) {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            if (validateUserEditingHisAccount(userInfo, userId, sessionTokenStr)) { // only the user can edit his own account
                    if (!userInfo.getName().equals(oldUsername)) {
                        throw new RuntimeException("Old username does not match current username.");
                    }
                    userInfo.setName(newUsername);
                    userRepo.store(userInfo); // Update the user info in the repository
                }
            else {
                throw new RuntimeException("Users can only edit their own accounts.");
            }
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to edit username: " + e.getMessage());
        }
    }

    public void editPassword(IUserRepo userRepo, String userId, String oldPassword, String newPassword,
            String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr)) {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            if (validateUserEditingHisAccount(userInfo, userId, sessionTokenStr)) {
                    if (!PasswordEncoderUtil.matches(oldPassword, userInfo.getPassword())) {
                        throw new RuntimeException("Old password does not match current password.");
                    }
                    String encryptedPass = PasswordEncoderUtil.encodePassword(newPassword);
                    userInfo.setPassword(encryptedPass);
                    userRepo.store(userInfo); // Update the user info in the repository
                }
            else {
                throw new RuntimeException("Users can only edit their own accounts.");
            }
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to edit password: " + e.getMessage());
        }
    }       

    public void editEmail(IUserRepo userRepo, String userId, String oldEmail, String newEmail, String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr)) {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            if (validateUserEditingHisAccount(userInfo, userId, sessionTokenStr)) { // only the user can edit his own account
                    if (!userInfo.getEmail().equals(oldEmail)) {
                        throw new RuntimeException("Old email does not match current email.");
                    }
                    userInfo.setEmail(newEmail);
                    userRepo.store(userInfo); // Update the user info in the repository
                }
            else {
                throw new RuntimeException("Users can only edit their own accounts.");
            }
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to edit email: " + e.getMessage());
        }
    }

    public void setUserGroupDiscount(IUserRepo userRepo, String userId, UserGroupDiscount userGroupDiscount,
            String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr)) {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            if (validateUserEditingHisAccount(userInfo, userId, sessionTokenStr)) { // only the user can edit his own account
                if (userGroupDiscount != null && userInfo.getUserGroupDiscount() != userGroupDiscount) {
                    userInfo.setUserGroupDiscount(userGroupDiscount);
                    userRepo.store(userInfo); // Update the user info in the repository
                }
                else {
                    throw new RuntimeException("Users can only edit their own accounts.");
                }
            }
            else {
                throw new RuntimeException("Users can only edit their own accounts.");
            }
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to set user group discount: " + e.getMessage());
        }
    }

    public void addProductionRole(IUserRepo userRepo, String userId, String productionId, String productionRole) {
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                userPublisher.publishUserNotFound(userId);
                return;
            }
            userInfo.getUserProduction().addProduction(productionId, productionRole);
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to add production role: " + e.getMessage());
        }
    }

    public void removeProductionRole(IUserRepo userRepo, String userId, String productionId) {
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                userPublisher.publishUserNotFound(userId);
                return;
            }
            userInfo.getUserProduction().removeProduction(productionId);
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to remove production role: " + e.getMessage());
        }
    }
}
            
