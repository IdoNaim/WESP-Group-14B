package com.ticketpurchasingsystem.project.domain.User;

import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;


public class UserHandler {

    public UserHandler() {
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

    public void registerUser(String userId, String name, String email, String password, UserGroupDiscount userGroupDiscount, IUserRepo userRepo) {
        // Implement logic to register a new user
        UserInfo userInfo = new UserInfo(userId, name, email, password, userGroupDiscount);
        try {
            userRepo.store(userInfo);
        } catch (Exception e) {
            // Handle exceptions (e.g., user already exists)
            throw new RuntimeException("Failed to register user: " + e.getMessage());
        }

    }

    public void handleGuestEntry(IUserRepo userRepo, SessionToken sessionToken) {
        String guestId = generateUniqueId();
        UserInfo guestUser = new UserInfo(guestId, sessionToken);
        try {
            userRepo.store(guestUser);
        } catch (Exception e) {
            // Handle exceptions (e.g., failed to store guest user)
            throw new RuntimeException("Failed to handle guest entry: " + e.getMessage());
        }
    }

    public void exitPlatform() {
        
    }

    public void loginUser(IUserRepo userRepo, String userId, String password) {
        // Implement logic to authenticate and log in the user
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null || !userInfo.password.equals(password)) {
                throw new RuntimeException("Invalid user ID or password.");
            }
            userInfo.LoggedIn = true;
            userRepo.store(userInfo); // Update the user info in the repository
        }
        catch (Exception e) {
            // Handle exceptions (e.g., user not found, incorrect password)
            throw new RuntimeException("Failed to log in user: " + e.getMessage());
        }
    }

    public void logoutUser(IUserRepo userRepo, String userId) {
        // Implement logic to log out the current user
        try {
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null || !userInfo.isLoggedIn()) {
                throw new RuntimeException("User is not logged in.");
            }
            userInfo.logout();
            userRepo.store(userInfo); // Update the user info in the repository
        } catch (Exception e) {
            // Handle exceptions (e.g., user not found, user not logged in)
            throw new RuntimeException("Failed to log out user: " + e.getMessage());
        }
    }


    public UserInfo getUserInfo(IUserRepo userRepo, String userId)
    {
        return userRepo.findByID(userId);
    }

    private String generateUniqueId() {
        // Implement logic to generate a unique ID for the user (e.g., using UUID)
        return null;
    }

}
