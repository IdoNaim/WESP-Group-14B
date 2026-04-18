package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.Optional;

public interface IAdminRepo {
    Optional<SystemAdmin> findById(String id);
    void save(SystemAdmin admin);
}
