package com.ticketpurchasingsystem.project.domain.tickets;

public class MinTicketsPurchasePolicy implements ITicketPurchaseRule {
    private final int minTickets;

    public MinTicketsPurchasePolicy(int minTickets) {
        this.minTickets = minTickets;
    }

    public int getMinTickets() {
        return minTickets;
    }

    @Override
    public PolicyValidationResult validate(TicketPurchaseContext context) {
        int requestedTickets = context.getRequestedTickets();
        if (requestedTickets < minTickets) {
            return PolicyValidationResult.fail("Requested tickets " + requestedTickets + " is less than the minimum limit of " + minTickets + ".");
        }
        return PolicyValidationResult.success();
    }
}
