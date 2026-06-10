package com.ticketpurchasingsystem.project.infrastructure;

import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DBSessionRepo  extends JpaRepository<SessionToken, String>  {

}
