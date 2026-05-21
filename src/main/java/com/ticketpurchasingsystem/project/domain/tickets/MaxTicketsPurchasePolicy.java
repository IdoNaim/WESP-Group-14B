package com.ticketpurchasingsystem.project.domain.tickets;

public class MaxTicketsPurchasePolicy implements ITicketPurchaseRule {
    private final int maxTickets;

    public MaxTicketsPurchasePolicy(int maxTickets) {
        this.maxTickets = maxTickets;
    }

    public int getMaxTickets() {
        return maxTickets;
    }

    @Override
    public PolicyValidationResult validate(TicketPurchaseContext context) {
        int requestedTickets = context.getRequestedTickets();
        if (requestedTickets > maxTickets) {
            return PolicyValidationResult.fail("Requested tickets " + requestedTickets + " exceeds the maximum limit of " + maxTickets + ".");
        }
        return PolicyValidationResult.success();
    }
}
