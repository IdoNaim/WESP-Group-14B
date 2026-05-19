package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy; // Added missing import

public class EmptySeatRule implements IPurchaseRule {
    private final boolean canLeaveEmptySeats;

    public EmptySeatRule(boolean canLeaveEmptySeats) {
        this.canLeaveEmptySeats = canLeaveEmptySeats;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        // If we cannot leave empty seats, and the seat is empty, return false.
        if (!canLeaveEmptySeats && context.isSeatEmpty()) {
            return false;
        }
        return true;
    }
}