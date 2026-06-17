package com.ticketpurchasingsystem.project.domain.User;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;


//** just a very early version of the user info class, we will add more fields and methods to it as we go along
// the user type can be hybrid can be owner on one group and founder of another, but for now we will just have one user type for each user, we will add more fields to the user info class as we go along, and we will also add more methods to it as we go along
// TODO
// Not sure where to put user premissions and roles, maybe we can have a separate class for that and link it to the user info class, or maybe we can just have a field in the user info class for that, we will decide on that later when we have a better understanding of the requirements and the design of the system
//  */
@Entity
@Table(name = "users")
public class UserInfo {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_state")
    private UserState userState;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_group_discount")
    private UserGroupDiscount userGroupDiscount;

    @Column(name = "logged_in")
    private boolean LoggedIn = false ;

    @Column(name = "session_token_str")
    private String sessionTokenStr;

    // Authoritative production-role data lives in the Production context
    // (users_production_companies + ProductionCompany.founderId, keyed by user_id).
    // On the User side it is a read-side projection, so it is not persisted here.
    @Transient
    private UserProduction userProduction;

    @JsonProperty("isAdmin")
    @Column(name = "is_admin")
    private boolean isAdmin = false;

    // Nullable on purpose: UserInfo has an app-assigned id (email / guest-uuid),
    // so Spring Data's isNew() relies on a null version to fire INSERT for new
    // users and UPDATE once the version has been set by Hibernate.
    @Version
    @Column(name = "version")
    private Integer version;

    // JPA requires a no-arg constructor
    protected UserInfo() {}

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

    public void setLoggedIn(boolean loggedIn) {
        this.LoggedIn = loggedIn;
    }

    public boolean isLoggedIn() {
        return LoggedIn;
    }

    public void setState(String newState) {
        switch (newState) {
            case "guest":
                this.userState = UserState.GUEST;
                break;
            case "member":
                this.userState = UserState.MEMBER;
                break;
            default:
                throw new IllegalArgumentException("Invalid user state: " + newState);
        }
    }

    public String getState() {
        return this.userState.toString();
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
    }
}
