package com.ticketpurchasingsystem.project.application.UserService;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;

import java.util.List;

public interface IUserService {

    public void enterPlatform() ;
    public void exitPlatform() ;

    public void registerUser(String userId, String password, String email, String name, UserGroupDiscount userGroupDiscount) ;
    public void loginUser(String userId, String password) ;
    public void logoutUser(String userId) ;

    public List<UserDTO> getAllUsers() ; 
    public UserDTO getUser(String userId) ; 
    public void deleteUser(String userId);

    public void editUsername(String userId, String oldUsername, String newUsername) ;
    public void editPassword(String userId, String oldPassword, String newPassword) ;
    public void editEmail(String userId, String oldEmail, String newEmail) ;

    public void addProductionId(String userId, String productionId) ;
    public void removeProductionId(String userId, String productionId) ;
    public void setUserGroupDiscount(String userId, UserGroupDiscount userGroupDiscount) ;

}

