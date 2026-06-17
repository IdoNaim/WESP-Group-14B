package com.ticketpurchasingsystem.project.domain.User;
import org.springframework.stereotype.Component;
import com.ticketpurchasingsystem.project.domain.Utils.PasswordEncoderUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class UserHandler {
    private static final int MIN_PASSWORD_LENGTH = 7;
    private static final String PASSWORD_RULE_MESSAGE =
            "Password must be at least 7 characters long and include both letters and numbers.";

    public UserHandler() {
    }

    public boolean isUserLoggedIn(UserInfo userInfo) {
        if (userInfo == null) {
            return false;
        }
        return userInfo.isLoggedIn();
    }

    public UserInfo registerUser(String userId, String name, String email, String password, UserGroupDiscount userGroupDiscount) {
        validateEmailFormat(email);
        validateNewPassword(password);
        String encryptedPass = PasswordEncoderUtil.encodePassword(password);
        return new UserInfo(userId, name, email, encryptedPass, userGroupDiscount);
    }

    public UserInfo handleGuestEntry(String sessionTokenStr, String guestId) {
        return new UserInfo(guestId, sessionTokenStr);
    }

    public void handleUserExit(UserInfo userInfo) {
        if (userInfo == null) {
            throw new RuntimeException("userInfo cannot be null.");
        }
        if (!userInfo.isGuest()){
            logoutUser(userInfo);
        }
    }

    public void loginUser(UserInfo userInfo, String password, String newSessionTokenStr) {
        if (userInfo == null || !PasswordEncoderUtil.matches(password, userInfo.getPassword())) {
            throw new RuntimeException("Invalid user ID or password.");
        }
        if (userInfo.isLoggedIn()) {
            throw new RuntimeException("User is already logged in.");
        }
        userInfo.setSessionTokenStr(newSessionTokenStr);
        userInfo.setLoggedIn(true);
    }

    public void logoutUser(UserInfo userInfo) {
        if (userInfo == null || !userInfo.isLoggedIn()) {
            throw new RuntimeException("User is not found or not logged in.");
        }
        userInfo.setLoggedIn(false);
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
        validateUserFound(userInfo);
        if (userInfo.getSessionTokenStr() == null || !userInfo.getSessionTokenStr().equals(sessionTokenStr) || !userInfo.getId().equals(userId)) {
            throw new RuntimeException("Users can only modify their own accounts.");
        }
    }

    public void editUsername(UserInfo userInfo, String userId, String oldUsername, String newUsername, String sessionTokenStr) {
        validateUserLoggedIn(userInfo);
        validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
        if (!userInfo.getName().equals(oldUsername)) {
            throw new RuntimeException("Old username does not match current username.");
        }
        userInfo.setName(newUsername);
    }

    public void editPassword(UserInfo userInfo, String userId, String oldPassword, String newPassword, String sessionTokenStr) {
        validateUserLoggedIn(userInfo);
        validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
        validateNewPassword(newPassword);
        if (!PasswordEncoderUtil.matches(oldPassword, userInfo.getPassword())) {
            throw new RuntimeException("Old password does not match current password.");
        }
        userInfo.setPassword(PasswordEncoderUtil.encodePassword(newPassword));
    }

    public void editEmail(UserInfo userInfo, String userId, String oldEmail, String newEmail, String sessionTokenStr) {
        validateUserLoggedIn(userInfo);
        validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
        validateEmailFormat(newEmail);
        if (!userInfo.getEmail().equals(oldEmail)) {
            throw new RuntimeException("Old email does not match current email.");
        }
        userInfo.setEmail(newEmail);
    }

    public void setUserGroupDiscount(UserInfo userInfo, String userId, UserGroupDiscount userGroupDiscount, String sessionTokenStr) {
        validateUserLoggedIn(userInfo);
        validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
        userInfo.setUserGroupDiscount(userGroupDiscount);
    }

    public void addProductionRole(UserInfo userInfo, Integer companyId, UserProduction.RoleInProduction role) {
        validateUserLoggedIn(userInfo);
        validateUserFound(userInfo);
        if (userInfo.getUserProduction() == null) {
            userInfo.setUserProduction(new UserProduction());
        }
        userInfo.getUserProduction().addProduction(companyId, role);
    }

    public void validateGuest(UserInfo userInfo) {
        if (userInfo == null) {
            throw new RuntimeException("Invalid guest info.");
        }
        if (!userInfo.isGuest()) {
            throw new RuntimeException("User is not a guest.");
        }
    }

    public void validateUserDoesNotExist(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        throw new RuntimeException("User with the same ID already exists.");
    }

    public void validateUserFound(UserInfo userInfo) {
        if (userInfo == null) {
            throw new RuntimeException("User not found.");
        }
    }

    public boolean isUserRegistered(UserInfo userInfo) {
        return userInfo != null && !userInfo.isGuest();
    }

    private void validateEmailFormat(String email) {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RuntimeException("Invalid email format.");
        }
    }

    private void validateNewPassword(String password) {
        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new RuntimeException(PASSWORD_RULE_MESSAGE);
        }
    }

    public void validateUserLoggedIn(UserInfo userInfo) {
        if (!userInfo.isLoggedIn()) {
            throw new RuntimeException("User must be logged in to perform this action.");
        }
    }

    public boolean isGuest(UserInfo userInfo) {
        if (userInfo == null) {
            throw new RuntimeException("User is null.");
        }
        return userInfo.isGuest();
    }
}
