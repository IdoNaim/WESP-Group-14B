package com.ticketpurchasingsystem.project.domain.User;
import java.util.List;

public interface IUserRepo {
    public void store(UserInfo userInfo) ;
    public void delete(String userId) ;
    public UserInfo findByID(String userId) ;
    public List<UserInfo> getAllUsers() ;
}
