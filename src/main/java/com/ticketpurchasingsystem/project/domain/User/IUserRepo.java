package com.ticketpurchasingsystem.project.domain.User;
import java.util.List;

<<<<<<< HEAD
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public interface IUserRepo {
    public void store(UserInfo userInfo) ;
    public void delete(String userId) ;
    public UserInfo findByID(String userId) ;
    public List<UserInfo> getAllUsers() ;
    public List<SessionToken> getAllGuests() ;
=======
import java.util.List;

public interface IUserRepo {
    List<UserInfo> findAll();
>>>>>>> 33_generate_Id
}
