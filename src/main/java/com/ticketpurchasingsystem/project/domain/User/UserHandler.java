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

    public String handleGuestEntry(IUserRepo userRepo) {
        String guestId = generateUniqueId();
        String sessionToken = authenticationService.login(guestId);
        UserInfo guestUser = new UserInfo(guestId, sessionToken);
        try {
            userRepo.store(guestUser);
            userPublisher.publishGuestEntered(guestId, sessionToken);
        } catch (Exception e) {
            // Handle exceptions (e.g., failed to store guest user)
            throw new RuntimeException("Failed to store guest entry, try again, error message: " + e.getMessage()); 
        }
        return guestId;
    }

    public void handleExit(IUserRepo userRepo, String sessionTokenStr) {
        if (!authenticationService.validate(sessionTokenStr)) {
            throw new RuntimeException("Invalid session token.");
        }
        try {
            String id = authenticationService.getUser(sessionTokenStr);
            UserInfo userInfo = userRepo.findByID(id);
            if (userInfo == null) {
                throw new RuntimeException("User not found.");
            }
            if (userInfo.getUserState() == UserState.GUEST) {
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

    private String generateUniqueId() {
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



}
