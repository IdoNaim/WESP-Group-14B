package com.ticketpurchasingsystem.project.infrastructure.persistence;

import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoJpaRepository extends JpaRepository<UserInfo, String> {
}
