package com.ticketpurchasingsystem.project.domain.User;

public class UserHandler {
    IUserRepo userRepo;

    public UserHandler(IUserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public boolean isUserLoggedIn() {
        // Implement logic to check if the user is logged in
        return false; // Placeholder return value
    }

    public User getCurrentUser() {
        // Implement logic to retrieve the current user
        return new User(); // Placeholder return value
    }

    public void registerUser(UserInfo userInfo) {
        // Implement logic to register a new user
    }

    public void loginUser(String username, String password) {
        // Implement logic to authenticate and log in the user
    }

    public void logoutUser() {
        // Implement logic to log out the current user
    }
    public UserInfo gUserInfo (User user)
    {
        return user.userInfo;
    }

}
