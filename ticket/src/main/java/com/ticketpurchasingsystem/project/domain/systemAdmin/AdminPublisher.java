package com.ticketpurchasingsystem.project.domain.systemAdmin;

import org.springframework.context.ApplicationEventPublisher;

class AdminPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public AdminPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
