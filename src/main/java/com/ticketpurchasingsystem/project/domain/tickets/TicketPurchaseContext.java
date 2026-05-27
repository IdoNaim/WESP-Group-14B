package com.ticketpurchasingsystem.project.domain.tickets;

public class TicketPurchaseContext {
    private final int buyerAge;
    private final int requestedTickets;

    public TicketPurchaseContext(int buyerAge, int requestedTickets) {
        this.buyerAge = buyerAge;
        this.requestedTickets = requestedTickets;
    }

    public int getBuyerAge() {
        return buyerAge;
    }

    public int getRequestedTickets() {
        return requestedTickets;
    }
}
