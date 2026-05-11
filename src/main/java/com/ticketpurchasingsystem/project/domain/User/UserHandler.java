package com.ticketpurchasingsystem.project.domain.User;

import org.springframework.stereotype.Component;
import com.ticketpurchasingsystem.project.domain.Utils.PasswordEncoderUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class UserHandler {

    public UserHandler() {
    }

    public boolean isUserLoggedIn(UserInfo userInfo) {
        if (userInfo == null) {
            return false;
        }
        return userInfo.isLoggedIn();
    }

    public UserInfo registerUser(String userId, String name, String email, String password, UserGroupDiscount userGroupDiscount) {
        String encryptedPass = PasswordEncoderUtil.encodePassword(password);
        return new UserInfo(userId, name, email, encryptedPass, userGroupDiscount);
    }

    public UserInfo handleGuestEntry(String sessionTokenStr, String guestId) {
        return new UserInfo(guestId, sessionTokenStr);
    }

    public void handleUserExit(UserInfo userInfo) {
        if (userInfo == null) {
            throw new RuntimeException("User not found.");
        }
        userInfo.LoggedIn = false;
        userInfo.setSessionTokenStr(null);
    }

    public void loginUser(UserInfo userInfo, String password, String newSessionTokenStr) {
        if (userInfo == null || !PasswordEncoderUtil.matches(password, userInfo.getPassword())) {
            throw new RuntimeException("Invalid user ID or password.");
        }
        if (userInfo.isLoggedIn()) {
            throw new RuntimeException("User is already logged in.");
        }
        userInfo.setSessionTokenStr(newSessionTokenStr);
        userInfo.LoggedIn = true;
    }

    public void logoutUser(UserInfo userInfo) {
        if (userInfo == null || !userInfo.isLoggedIn()) {
            throw new RuntimeException("User is not found or not logged in.");
        }
        userInfo.LoggedIn = false;
        userInfo.setSessionTokenStr(null);
    }

    public UserDTO mapToDTO(UserInfo userInfo) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(userInfo, UserDTO.class);
    }

    public String generateUniqueId() {
        return java.util.UUID.randomUUID().toString();
    }

    public void validateUserEditingHisAccount(UserInfo userInfo, String userId, String sessionTokenStr) {
        if (userInfo == null) {
            throw new RuntimeException("User not found.");
        }
        if (userInfo.getSessionTokenStr() == null || !userInfo.getSessionTokenStr().equals(sessionTokenStr) || !userInfo.getId().equals(userId)) {
            throw new RuntimeException("Users can only modify their own accounts.");
        }
    }

    public void editUsername(UserInfo userInfo, String userId, String oldUsername, String newUsername, String sessionTokenStr) {
        validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
        if (!userInfo.getName().equals(oldUsername)) {
            throw new RuntimeException("Old username does not match current username.");
        }
        userInfo.setName(newUsername);
    }

    public void editPassword(UserInfo userInfo, String userId, String oldPassword, String newPassword, String sessionTokenStr) {
        validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
        if (!PasswordEncoderUtil.matches(oldPassword, userInfo.getPassword())) {
            throw new RuntimeException("Old password does not match current password.");
        }
        userInfo.setPassword(PasswordEncoderUtil.encodePassword(newPassword));
    }

    public void editEmail(UserInfo userInfo, String userId, String oldEmail, String newEmail, String sessionTokenStr) {
        validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
        if (!userInfo.getEmail().equals(oldEmail)) {
            throw new RuntimeException("Old email does not match current email.");
        }
        userInfo.setEmail(newEmail);
    }

    public void setUserGroupDiscount(UserInfo userInfo, String userId, UserGroupDiscount userGroupDiscount, String sessionTokenStr) {
        validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
        userInfo.setUserGroupDiscount(userGroupDiscount);
    }

    public void addProductionRole(UserInfo userInfo, Integer companyId, UserProduction.RoleInProduction role) {
        if (userInfo.getUserProduction() == null) {
            userInfo.setUserProduction(new UserProduction());
        }
        userInfo.getUserProduction().addProduction(companyId, role);
    }
}
