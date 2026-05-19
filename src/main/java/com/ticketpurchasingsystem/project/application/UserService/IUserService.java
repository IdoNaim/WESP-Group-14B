package com.ticketpurchasingsystem.project.application.UserService;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;

import java.util.List;

public interface IUserService {

    public void guestEntry() ;
    public void Exit(String sessionTokenStr) ;

    public void registerUser(String userId, String name, String password, String email, UserGroupDiscount userGroupDiscount, String sessionTokenStr) ;
    public String loginUser(String userId, String password, String sessionTokenStr) ;
    public void logoutUser(String userId, String sessionTokenStr) ;

    public List<UserDTO> getAllUsers() ; 
    public UserDTO getUser(String userId) ; 
    public void deleteUser(String userId, String sessionTokenStr) ;

    public void editUsername(String userId, String oldUsername, String newUsername, String sessionTokenStr) ;
    public void editPassword(String userId, String oldPassword, String newPassword, String sessionTokenStr) ;
    public void editEmail(String userId, String oldEmail, String newEmail, String sessionTokenStr) ;

    public void setUserGroupDiscount(String userId, UserGroupDiscount userGroupDiscount, String sessionTokenStr) ;

}

