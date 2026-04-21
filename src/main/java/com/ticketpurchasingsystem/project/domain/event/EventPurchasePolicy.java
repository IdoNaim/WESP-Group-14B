package com.ticketpurchasingsystem.project.domain.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * EventPurchasePolicy is a Value Object belonging to the Event aggregate.
 * It uses a composite of rules to validate a purchase attempt, making it 
 * flexible for both production companies and specific events.
 */
public class EventPurchasePolicy {

    private final List<PurchaseRule> rules;

    public EventPurchasePolicy(List<PurchaseRule> rules) {
        this.rules = rules != null ? new ArrayList<>(rules) : Collections.emptyList();
    }

    /**
     * Validates a purchase attempt against all configured rules.
     */
    public void validatePurchase(PurchaseContext context) {
        for (PurchaseRule rule : rules) {
            rule.validate(context);
        }
    }
    
    public List<PurchaseRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
}

/**
 * Context of a purchase attempt.
 * Comes from the application/service layer.
 */
class PurchaseContext {

    private final int ticketAmount;
    private final int userAge;
    private final Set<String> userGroups;
    private final String purchaseRoute; // e.g., "ONLINE", "BOX_OFFICE", "PRESALE"
    private final boolean leavesSingleOrphanSeat; // Calculated by the seating/venue domain service

    public PurchaseContext(int ticketAmount, int userAge, Set<String> userGroups, String purchaseRoute, boolean leavesSingleOrphanSeat) {
        this.ticketAmount = ticketAmount;
        this.userAge = userAge;
        this.userGroups = userGroups != null ? userGroups : Collections.emptySet();
        this.purchaseRoute = purchaseRoute;
        this.leavesSingleOrphanSeat = leavesSingleOrphanSeat;
    }

    public int getTicketAmount() { return ticketAmount; }
    public int getUserAge() { return userAge; }
    public Set<String> getUserGroups() { return userGroups; }
    public String getPurchaseRoute() { return purchaseRoute; }
    public boolean isLeavesSingleOrphanSeat() { return leavesSingleOrphanSeat; }
}

/**
 * Base interface for all purchase rules.
 */
interface PurchaseRule {
    void validate(PurchaseContext context);
}

/**
 * Minimum ticket constraint rule.
 */
class MinTicketsRule implements PurchaseRule {
    private final int minTickets;

    public MinTicketsRule(int minTickets) { this.minTickets = minTickets; }

    @Override
    public void validate(PurchaseContext context) {
        if (context.getTicketAmount() < minTickets) {
            throw new IllegalArgumentException("Minimum ticket amount not met.");
        }
    }
}

/**
 * Maximum ticket constraint rule.
 */
class MaxTicketsRule implements PurchaseRule {
    private final int maxTickets;

    public MaxTicketsRule(int maxTickets) { this.maxTickets = maxTickets; }

    @Override
    public void validate(PurchaseContext context) {
        if (context.getTicketAmount() > maxTickets) {
            throw new IllegalArgumentException("Maximum ticket amount exceeded.");
        }
    }
}

/**
 * Minimum age constraint rule.
 */
class MinAgeRule implements PurchaseRule {
    private final int minAge;

    public MinAgeRule(int minAge) { this.minAge = minAge; }

    @Override
    public void validate(PurchaseContext context) {
        if (context.getUserAge() < minAge) {
            throw new IllegalArgumentException("User does not meet minimum age requirement.");
        }
    }
}

/**
 * Maximum age constraint rule.
 */
class MaxAgeRule implements PurchaseRule {
    private final int maxAge;

    public MaxAgeRule(int maxAge) { this.maxAge = maxAge; }

    @Override
    public void validate(PurchaseContext context) {
        if (context.getUserAge() > maxAge) {
            throw new IllegalArgumentException("User exceeds maximum age limit.");
        }
    }
}

/**
 * Allowed user groups rule (e.g., VIP, Student, General).
 */
class AllowedGroupsRule implements PurchaseRule {
    private final Set<String> allowedGroups;

    public AllowedGroupsRule(Set<String> allowedGroups) {
        this.allowedGroups = allowedGroups != null ? allowedGroups : Collections.emptySet();
    }

    @Override
    public void validate(PurchaseContext context) {
        if (allowedGroups.isEmpty()) return; // If no specific groups defined, assume all are allowed.
        
        boolean allowed = context.getUserGroups().stream()
                .anyMatch(allowedGroups::contains);

        if (!allowed) {
            throw new IllegalArgumentException("User is not in an allowed group to purchase these tickets.");
        }
    }
}

/**
 * Allowed purchase routes rule (e.g., can only be bought at Box Office).
 */
class AllowedRoutesRule implements PurchaseRule {
    private final Set<String> allowedRoutes;

    public AllowedRoutesRule(Set<String> allowedRoutes) {
        this.allowedRoutes = allowedRoutes != null ? allowedRoutes : Collections.emptySet();
    }

    @Override
    public void validate(PurchaseContext context) {
        if (allowedRoutes.isEmpty()) return; 

        if (!allowedRoutes.contains(context.getPurchaseRoute())) {
            throw new IllegalArgumentException("Purchase route '" + context.getPurchaseRoute() + "' is not allowed for this event.");
        }
    }
}

/**
 * Prohibition on leaving a single empty seat.
 */
class NoOrphanSeatRule implements PurchaseRule {
    @Override
    public void validate(PurchaseContext context) {
        if (context.isLeavesSingleOrphanSeat()) {
            throw new IllegalArgumentException("Seat selection is invalid: it leaves a single empty seat behind.");
        }
    }
}