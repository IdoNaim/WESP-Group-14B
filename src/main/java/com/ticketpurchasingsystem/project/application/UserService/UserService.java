package com.ticketpurchasingsystem.project.application.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserHandler;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.User.UserProduction;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

import jakarta.transaction.Transactional;
@Service
@Transactional
public class UserService implements IUserService {

    private final IUserRepo userRepo;
    private final UserHandler userHandler;
    private final AuthenticationService authenticationService;
    private final UserPublisher userPublisher;

    @Autowired
    public UserService(IUserRepo userRepo, UserHandler userHandler, AuthenticationService authenticationService, UserPublisher userPublisher) {
        this.userRepo = userRepo;
        this.userHandler = userHandler;
        this.authenticationService = authenticationService;
        this.userPublisher = userPublisher;

        // Seed the default admin once. SystemAdminService also seeds "admin-1" at
        // startup, so guard on existence to avoid a duplicate insert against the DB repo.
        if (userRepo.findByID("admin-1") == null) {
            UserInfo newUser = userHandler.registerUser("admin-1", "Admin", "admin@gmail.com", "admin123", UserGroupDiscount.NONE);
            if (newUser != null) {
                newUser.setAdmin(true);
                userRepo.store(newUser);
            }
        }
        // UserInfo idonaim = userHandler.registerUser("idonaim56@gmail.com", "Ido Naim", "idonaim56@gmail.com", "idonaim56", UserGroupDiscount.NONE);
        // UserProduction userProduction = new UserProduction();
        // userProduction.addProduction(1, UserProduction.RoleInProduction.FOUNDER);
        // if( idonaim != null){            idonaim.setUserProduction(userProduction);
        //     }
        //     userRepo.store(idonaim);

    }


    public String guestEntry() {
        try {
            String uniqueGuestId = userHandler.generateUniqueId();
            String sessionToken = authenticationService.login(uniqueGuestId);
            UserInfo guest = userHandler.handleGuestEntry(sessionToken, uniqueGuestId);
            userRepo.store(guest);
            userPublisher.publishGuestEntered(guest.getId(), sessionToken);
            loggerDef.getInstance().info("Guest entry successful. Guest ID: " + guest.getId());
            return sessionToken;
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to handle guest entry: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public void Exit(String sessionTokenStr) {
        try {
            if (authenticationService.validate(sessionTokenStr)) {
                String userId = authenticationService.getUser(sessionTokenStr);
                UserInfo userInfo = userRepo.findByID(userId);
                try {
                    userHandler.handleUserExit(userInfo);
                } catch (Exception e) {
                    loggerDef.getInstance().error("User is null");
                    authenticationService.logout(sessionTokenStr);
                    return;
                }
                // if we are here user info is not null, valid guest or user is leaving
                if (userInfo.isGuest()) {
                    userRepo.delete(userId);
                    userPublisher.publishGuestExited(userId, sessionTokenStr);
                } else {
                    userRepo.store(userInfo);
                    userPublisher.publishUserLoggedOut(userId, sessionTokenStr);
                }

                authenticationService.logout(sessionTokenStr);
                loggerDef.getInstance().info("Exit successful for token/user.");
            }
            else {
                loggerDef.getInstance().warn("Invalid session token provided for exit.");
                throw new RuntimeException("Invalid session token: " + sessionTokenStr);
            }
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to handle exit: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Safety net for an "irregular exit": a guest who leaves without calling
     * {@link #Exit} (e.g. just closes the browser tab). Their JWT eventually
     * expires, but the SessionToken row and the guest UserInfo row have no
     * synchronous trigger to remove them, so this scheduled sweep purges every
     * expired session together with the guest accounts tied to one. A registered
     * member keeps their account, but if their own session expired we also reset
     * {@code logged_in}/{@code sessionTokenStr} so a stale login can't lock them out
     * (this is the backstop for the WebSocket disconnect path — see
     * {@link #handleDisconnect(String, String)} — in case that event was missed,
     * e.g. a server restart dropped the in-memory presence registry).
     * The interval is configurable via {@code guest.session.cleanup.interval-ms}
     * (default 30 minutes); {@code @Scheduled} ignores the return value, which is
     * returned so the sweep can be invoked and asserted on directly in tests.
     *
     * @return the number of expired sessions removed
     */
    @Scheduled(fixedDelayString = "${guest.session.cleanup.interval-ms:1800000}")
    public int purgeExpiredSessions() {
        List<String> expiredTokens = authenticationService.getExpiredSessionTokens();
        if (expiredTokens.isEmpty()) {
            return 0;
        }
        Set<String> expiredTokenSet = new HashSet<>(expiredTokens);

        for (UserInfo user : userRepo.getAllUsers()) {
            if (user.getSessionTokenStr() == null
                    || !expiredTokenSet.contains(user.getSessionTokenStr())) {
                continue;
            }
            if (Boolean.TRUE.equals(user.isGuest())) {
                // Orphaned guest whose session expired: remove the whole account.
                userRepo.delete(user.getId());
                userPublisher.publishGuestExited(user.getId(), user.getSessionTokenStr());
            } else {
                // Member with an expired session: keep the account but log them out
                // so the stale flag can't block their next login.
                String token = user.getSessionTokenStr();
                user.setLoggedIn(false);
                user.setSessionTokenStr(null);
                userRepo.store(user);
                userPublisher.publishUserLoggedOut(user.getId(), token);
            }
        }

        for (String token : expiredTokens) {
            authenticationService.removeSessionManually(token);
        }
        loggerDef.getInstance().info("Session cleanup purged " + expiredTokens.size() + " expired session(s).");
        return expiredTokens.size();
    }

    /**
     * Reacts to an irregular exit detected by the presence WebSocket: the user
     * closed the tab / lost the connection without calling {@link #Exit}. The
     * WebSocket layer (infrastructure) publishes a {@code UserLeavedPlatformEvent};
     * a listener routes it here. This keeps WebSocket details out of the domain.
     *
     * <p>Idempotent and race-safe: it acts only if {@code token} is still the
     * user's current session. If the user already logged in again (session
     * takeover gave them a new token), a late close of the old socket is ignored.
     *
     * @param userId the user whose connection dropped
     * @param token  the session token that was attached to the dropped connection
     */
    public void handleDisconnect(String userId, String token) {
        try {
            if (userId == null || token == null) {
                return;
            }
            UserInfo userInfo = userRepo.findByID(userId);
            if (userInfo == null) {
                return; // already gone (e.g. a guest already purged)
            }
            // Only react to the user's CURRENT session; ignore a late close of a
            // session the user has already replaced by logging in again.
            if (!token.equals(userInfo.getSessionTokenStr())) {
                return;
            }

            if (userInfo.isGuest()) {
                userRepo.delete(userId);
                authenticationService.removeSessionManually(token);
                userPublisher.publishGuestExited(userId, token);
            } else {
                userInfo.setLoggedIn(false);
                userInfo.setSessionTokenStr(null);
                userRepo.store(userInfo);
                authenticationService.removeSessionManually(token);
                userPublisher.publishUserLoggedOut(userId, token);
            }
            loggerDef.getInstance().info("Handled irregular disconnect for user: " + userId);
        } catch (Exception e) {
            // A disconnect handler must never propagate: swallow and log so a stray
            // failure can't disrupt the WebSocket close callback / event publisher.
            loggerDef.getInstance().error("Failed to handle disconnect for user " + userId + ": " + e.getMessage());
        }
    }

    public void registerUser(String userId, String name, String password, String email, UserGroupDiscount userGroupDiscount, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            userHandler.validateUserDoesNotExist(userRepo.findByID(userId));
            // if we are here the user id is not taken
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
            String guestId = authenticationService.getUser(sessionTokenStr);
            UserInfo guestInfo = userRepo.findByID(guestId);
            userHandler.validateGuest(guestInfo);
            // if we are here, it means that session token is valid
            // the user is not null and a guest so he can login and become a user 
            UserInfo userInfo = userRepo.findByID(userId);
            userHandler.validateUserFound(userInfo);
            // validate user exists

            // Capture any session left over from a previous (possibly irregular) exit,
            // so we can drop its stale row once the new login succeeds (session takeover).
            String staleToken = userInfo.getSessionTokenStr();
            boolean wasLoggedIn = userInfo.isLoggedIn();

            // Generate a fresh session token via auth service
            String newSessionTokenStr = authenticationService.login(userId);

            // Let handler update login status and state
            userHandler.loginUser(userInfo, password, newSessionTokenStr);

            // Take over: remove the member's previous session row so it doesn't linger.
            if (wasLoggedIn && staleToken != null && !staleToken.equals(newSessionTokenStr)) {
                authenticationService.removeSessionManually(staleToken);
            }

            // Delete guest matching the OLD session token before saving the user
            userRepo.delete(guestId); // if we are here, it means that session token is valid and the user was a guest before login, so we can delete him by the guestId we got from the session token
            authenticationService.logout(sessionTokenStr);
            userPublisher.publishGuestExited(guestId, sessionTokenStr);

            userRepo.store(userInfo); // Store the updated user info with new session token and logged-in status
            userPublisher.publishUserLoggedIn(userId, newSessionTokenStr);
            loggerDef.getInstance().info("User logged in successfully: " + userId);
            return newSessionTokenStr;
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to log in user: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public String loginAdmin(String userId, String password, String sessionTokenStr){
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            String guestId = authenticationService.getUser(sessionTokenStr);
            UserInfo guestInfo = userRepo.findByID(guestId);

            userHandler.validateGuest(guestInfo);
            UserInfo userInfo = userRepo.findByID("admin-1");
            System.out.println("searched admin");
            userHandler.validateUserFound(userInfo);
            System.out.println("found admin");
            // if we are here, it means that session token is valid
            // the user is not null and a guest so he can login and become a user

            // Capture any leftover session for takeover (see loginUser).
            String staleToken = userInfo.getSessionTokenStr();
            boolean wasLoggedIn = userInfo.isLoggedIn();

            String newSessionTokenStr = null;
            // validate user exists
            if(userId.equals("admin@gmail.com") && password.equals("admin123")){
                newSessionTokenStr = authenticationService.login("admin-1", "admin");
            }else{
                throw new RuntimeException("Failed to log in user "+ userId);
            }
            userHandler.loginUser(userInfo, password, newSessionTokenStr);
            // Take over: remove the admin's previous session row so it doesn't linger.
            if (wasLoggedIn && staleToken != null && !staleToken.equals(newSessionTokenStr)) {
                authenticationService.removeSessionManually(staleToken);
            }
            // Delete guest matching the OLD session token before saving the user
            userRepo.delete(guestId); // if we are here, it means that session token is valid and the user was a guest before login, so we can delete him by the guestId we got from the session token
            authenticationService.logout(sessionTokenStr);
            userPublisher.publishGuestExited(guestId, sessionTokenStr);
            loggerDef.getInstance().info("Admin logged in successfully: " + userId);
            return newSessionTokenStr;
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to log in admin: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String logoutUser(String userId, String sessionToken) {
        try {
            if (!authenticationService.validate(sessionToken)) {
                throw new RuntimeException("Invalid session token.");
            }

            UserInfo userInfo = userRepo.findByID(userId);
            userHandler.validateUserFound(userInfo);
            userHandler.validateUserLoggedIn(userInfo);
            userHandler.logoutUser(userInfo);
            userRepo.store(userInfo);
            userPublisher.publishUserLoggedOut(userId, sessionToken);

            authenticationService.logout(sessionToken);
            String newGuestSession = authenticationService.login(userHandler.generateUniqueId());
            UserInfo guest = userHandler.handleGuestEntry(newGuestSession, authenticationService.getUser(newGuestSession));
            userRepo.store(guest);
            userPublisher.publishGuestEntered(guest.getId(), newGuestSession);
            loggerDef.getInstance().info("User logged out successfully: " + userId);
            return newGuestSession;
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
        userHandler.validateUserFound(userInfo);
        return userHandler.mapToDTO(userInfo);
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        UserInfo userInfo = userRepo.findByID(userId);
        userHandler.validateUserFound(userInfo);
        return userInfo;
    }

    public void deleteUser(String userId, String sessionTokenStr) {
        try {
            if (!authenticationService.validate(sessionTokenStr)) {
                throw new RuntimeException("Invalid session token.");
            }
            UserInfo userInfo = userRepo.findByID(userId);
            userHandler.validateUserEditingHisAccount(userInfo, userId, sessionTokenStr);
            userHandler.validateUserLoggedIn(userInfo);
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
            throw new RuntimeException(e.getMessage(), e);
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
            throw new RuntimeException(e.getMessage(), e);
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
            throw new RuntimeException(e.getMessage(), e);
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
            userHandler.addProductionRole(userInfo, companyId, role);
            userRepo.store(userInfo);
            loggerDef.getInstance().info("Production role " + role + " assigned successfully to user: " + userId + " for company ID: " + companyId);
        } catch (Exception e) {
             loggerDef.getInstance().error("Failed to assign production role: " + e.getMessage());
        }
    }
    public boolean isUserRegistered(String userId){
        return userHandler.isUserRegistered(userRepo.findByID(userId));
    }

    public boolean isGuest(String userId) {
        try {
            return userHandler.isGuest(userRepo.findByID(userId));
        } catch (Exception e) {
            loggerDef.getInstance().error("Failed to check if user is guest (probably user not found): " + e.getMessage());
            return false;
        }
    }
}
