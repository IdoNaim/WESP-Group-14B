package com.ticketpurchasingsystem.project.domain.User;

import org.springframework.boot.autoconfigure.jms.JmsProperties.Listener.Session;

import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

//** just a very early version of the user info class, we will add more fields and methods to it as we go along
// the user type can be hybrid can be owner on one group and founder of another, but for now we will just have one user type for each user, we will add more fields to the user info class as we go along, and we will also add more methods to it as we go along
// TODO
// Not sure where to put user premissions and roles, maybe we can have a separate class for that and link it to the user info class, or maybe we can just have a field in the user info class for that, we will decide on that later when we have a better understanding of the requirements and the design of the system
//  */
public class UserInfo {
    String id;
    String name;
    String email;
    String password;
    UserState userState;
    UserGroupDiscount userGroupDiscount;
    boolean LoggedIn = false ;
    String sessionTokenStr;
    UserProduction userProduction;

    // registration
    public UserInfo(String id, String name, String email, String password, UserGroupDiscount userGroupDiscount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.userState = UserState.MEMBER;
        this.userGroupDiscount = userGroupDiscount;
        this.LoggedIn = false;
        this.sessionTokenStr = null; // No session token for registered users until they log in
        this.userProduction = new UserProduction();
    }

    // guest
    public UserInfo(String id, String sessionTokenStr) {
        this.id = id; 
        this.name = "";
        this.email = "";
        this.password = "";
        this.userState = UserState.GUEST;
        this.userGroupDiscount = UserGroupDiscount.NONE;
        this.sessionTokenStr = sessionTokenStr;
        this.LoggedIn = false;
        this.userProduction = null; // Guests do not have production roles
    }

    public boolean isLoggedIn() {
        return LoggedIn;
    }

    public void setUserGroupDiscount(UserGroupDiscount userGroupDiscount) {
        if (!isLoggedIn()) {
            throw new IllegalStateException("User must be logged in to set group discount.");
        }
        if (userGroupDiscount == null) {
            this.userGroupDiscount = UserGroupDiscount.NONE;
        } else {
            this.userGroupDiscount = userGroupDiscount;
        }
    }
    
    public UserGroupDiscount getUserGroupDiscount() {
        return userGroupDiscount;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public UserState getUserState() {
        return userState;
    }
    
    public void setUserState(UserState userState) {
        this.userState = userState;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean isGuest() {
        return this.userState == UserState.GUEST;
    }
    
    public String getSessionTokenStr() {
        return sessionTokenStr;
    }

    public void setSessionTokenStr(String sessionTokenStr) {
        this.sessionTokenStr = sessionTokenStr;
    }

    public UserProduction getUserProduction() {
        return userProduction;
    }

    public void setUserProduction(UserProduction userProduction) {
        this.userProduction = userProduction;
    }
}
