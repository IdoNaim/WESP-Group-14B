package com.ticketpurchasingsystem.project.domain.event;

import java.util.Collections;
import java.util.Set;
/**
 * EventPurchasePolicy is a Value Object belonging to the Event aggregate.
 * It defines all rules that must be satisfied before a ticket purchase is allowed.
 */
public class EventPurchasePolicy {

    // Using Integer to allow nulls in case a specific rule is not applied to an event
    private final Integer minTickets;
    private final Integer maxTickets;
    private final Integer minAge;
    private final Integer maxAge;
    private final Set<String> allowedGroups;

    public EventPurchasePolicy(Integer minTickets, Integer maxTickets, Integer minAge, Integer maxAge, Set<String> allowedGroups) {
        validateConsistency(minTickets, maxTickets, minAge, maxAge);
        
        this.minTickets = minTickets;
        this.maxTickets = maxTickets;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.allowedGroups = allowedGroups == null ? Collections.emptySet() : Set.copyOf(allowedGroups);
    }

    /**
     * Ensures there are no conflicting constraints.
     */
    private void validateConsistency(Integer minTickets, Integer maxTickets, Integer minAge, Integer maxAge) {
        if (minTickets != null && maxTickets != null && minTickets > maxTickets) {
            throw new IllegalArgumentException("Conflicting ticket rules: minTickets > maxTickets");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new IllegalArgumentException("Conflicting age rules: minAge > maxAge");
        }
    }

    /**
     * Validates a purchase attempt against all configured rules.
     */
    public void validatePurchase(PurchaseContext context) {
        if (minTickets != null && context.getTicketAmount() < minTickets) {
            throw new IllegalArgumentException("Minimum ticket amount not met");
        }
        if (maxTickets != null && context.getTicketAmount() > maxTickets) {
            throw new IllegalArgumentException("Maximum ticket amount exceeded");
        }
        if (minAge != null && context.getUserAge() < minAge) {
            throw new IllegalArgumentException("User does not meet minimum age requirement");
        }
        if (maxAge != null && context.getUserAge() > maxAge) {
            throw new IllegalArgumentException("User exceeds maximum age limit");
        }
        
        if (!allowedGroups.isEmpty()) {
            boolean allowed = context.getUserGroups().stream()
                    .anyMatch(allowedGroups::contains);
            if (!allowed) {
                throw new IllegalArgumentException("User is not allowed to purchase tickets");
            }
        }
    }

    // Getters
    public Integer getMinTickets() { return minTickets; }
    public Integer getMaxTickets() { return maxTickets; }
    public Integer getMinAge() { return minAge; }
    public Integer getMaxAge() { return maxAge; }
    public Set<String> getAllowedGroups() { return allowedGroups; }
}
/**
 * Context of a purchase attempt.
 * Comes from application/service layer.
 */
class PurchaseContext {

    private final int ticketAmount;
    private final int userAge;
    private final Set<String> userGroups;

    public PurchaseContext(int ticketAmount, int userAge, Set<String> userGroups) {
        this.ticketAmount = ticketAmount;
        this.userAge = userAge;
        this.userGroups = userGroups;
    }

    public int getTicketAmount() {
        return ticketAmount;
    }

    public int getUserAge() {
        return userAge;
    }

    public Set<String> getUserGroups() {
        return userGroups;
    }
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

    public MinTicketsRule(int minTickets) {
        this.minTickets = minTickets;
    }

    @Override
    public void validate(PurchaseContext context) {
        if (context.getTicketAmount() < minTickets) {
            throw new IllegalArgumentException("Minimum ticket amount not met");
        }
    }

    public int getMinTickets() {
        return minTickets;
    }
}


/**
 * Maximum ticket constraint rule.
 */
class MaxTicketsRule implements PurchaseRule {

    private final int maxTickets;

    public MaxTicketsRule(int maxTickets) {
        this.maxTickets = maxTickets;
    }

    @Override
    public void validate(PurchaseContext context) {
        if (context.getTicketAmount() > maxTickets) {
            throw new IllegalArgumentException("Maximum ticket amount exceeded");
        }
    }

    public int getMaxTickets() {
        return maxTickets;
    }
}


/**
 * Minimum age constraint rule.
 */
class MinAgeRule implements PurchaseRule {

    private final int minAge;

    public MinAgeRule(int minAge) {
        this.minAge = minAge;
    }

    @Override
    public void validate(PurchaseContext context) {
        if (context.getUserAge() < minAge) {
            throw new IllegalArgumentException("User does not meet minimum age requirement");
        }
    }

    public int getMinAge() {
        return minAge;
    }
}


/**
 * Maximum age constraint rule.
 */
class MaxAgeRule implements PurchaseRule {

    private final int maxAge;

    public MaxAgeRule(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public void validate(PurchaseContext context) {
        if (context.getUserAge() > maxAge) {
            throw new IllegalArgumentException("User exceeds maximum age limit");
        }
    }

    public int getMaxAge() {
        return maxAge;
    }
}


/**
 * Allowed user groups rule.
 */
class AllowedGroupsRule implements PurchaseRule {

    private final Set<String> allowedGroups;

    public AllowedGroupsRule(Set<String> allowedGroups) {
        this.allowedGroups = allowedGroups;
    }

    @Override
    public void validate(PurchaseContext context) {

        boolean allowed = context.getUserGroups()
                .stream()
                .anyMatch(allowedGroups::contains);

        if (!allowed) {
            throw new IllegalArgumentException("User is not allowed to purchase tickets");
        }
    }
}