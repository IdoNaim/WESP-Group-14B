package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class DBUserRepo implements IUserRepo {

    private final UserInfoJpaRepository userInfoJpaRepository;

    @Autowired
    public DBUserRepo(UserInfoJpaRepository userInfoJpaRepository) {
        this.userInfoJpaRepository = userInfoJpaRepository;
    }

    @Override
    public void store(UserInfo userInfo) {
        // UserInfo has an app-assigned id with a nullable @Version, so Spring Data
        // INSERTs new users (version == null) and UPDATEs once Hibernate has set the
        // version. Callers updating an existing user pass the entity they fetched
        // (version already set); the admin-1 seed is guarded at the seed sites so it
        // is only inserted once.
        userInfoJpaRepository.save(userInfo);
    }

    @Override
    public void delete(String userId) {
        // Deletes the record from the database by its primary key string
        userInfoJpaRepository.deleteById(userId);
    }

    @Override
    public UserInfo findByID(String userId) {
        // Unwraps the Optional container for the domain layer
        return userInfoJpaRepository.findById(userId).orElse(null);
    }

    @Override
    public List<UserInfo> getAllUsers() {
        return userInfoJpaRepository.findAll();
    }

    @Override
    public boolean isAdmin(String userId) {
        return userInfoJpaRepository.findById(userId).map(UserInfo::isAdmin).orElse(false);
    }

    @Override
    public void deleteAll() {
        userInfoJpaRepository.deleteAll();
    }
}
