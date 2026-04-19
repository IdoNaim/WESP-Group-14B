package com.ticketpurchasingsystem.project.application;

public interface  UserService {


    public String registerUser(String username, String password) ;
    public String loginUser(String username, String password) ;
    public void logoutUser(String sessionToken) ;
    public void getUsers() ;
    public void deleteUser(String userId);
    public void getUser(String userId) ;




    
}
