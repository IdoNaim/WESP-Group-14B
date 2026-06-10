package com.ticketpurchasingsystem.project.infrastructure;

import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public interface DBSessionRepo  extends JpaRepository<SessionToken, String>, ISessionRepo  {
    //save is in JpaRepository
    Optional<SessionToken> findByToken(String token);

    void deleteByToken(String token);

    List<SessionToken> findAll();
}
