package com.ticketpurchasingsystem.project.domain.User;


//** just a very early version of the user info class, we will add more fields and methods to it as we go along
// the user type can be hybrid can be owner on one group and founder of another, but for now we will just have one user type for each user, we will add more fields to the user info class as we go along, and we will also add more methods to it as we go along
// TODO
// Not sure where to put user premissions and roles, maybe we can have a separate class for that and link it to the user info class, or maybe we can just have a field in the user info class for that, we will decide on that later when we have a better understanding of the requirements and the design of the system
//  */
public class UserInfo {
    String name;
    String email;
    String password;
    UserState userType;
    UserGroupDiscount userGroupDiscount;

    public UserInfo(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public UserInfo() {
        this.name = "";
        this.email = "";
        this.password = "";
    }

     public String getName() {
        return name;
    }
    public void setUserGroupDiscount(UserGroupDiscount userGroupDiscount) {
        this.userGroupDiscount = userGroupDiscount;
    }

     public UserGroupDiscount getUserGroupDiscount() {
        return userGroupDiscount;
    }
    public void setUserType(UserState userType) {
        this.userType = userType;
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
    
}
