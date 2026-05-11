package com.ticketpurchasingsystem.project.domain.Production;

public class OptimisticLockingFailureException extends RuntimeException {

    public OptimisticLockingFailureException(String message) {
        super(message);
    }
}
