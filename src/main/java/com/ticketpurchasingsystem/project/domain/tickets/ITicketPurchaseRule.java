package com.ticketpurchasingsystem.project.domain.tickets;

@FunctionalInterface
public interface ITicketPurchaseRule {
    PolicyValidationResult validate(TicketPurchaseContext context);
}
