package com.ticketpurchasingsystem.project.domain.tickets;

public class AgePurchasePolicy implements ITicketPurchaseRule {
    private final Integer minAge;
    private final Integer maxAge;

    public AgePurchasePolicy(Integer minAge, Integer maxAge) {
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    @Override
    public PolicyValidationResult validate(TicketPurchaseContext context) {
        int buyerAge = context.getBuyerAge();
        if (minAge != null && buyerAge < minAge) {
            return PolicyValidationResult.fail("Buyer age " + buyerAge + " is less than the minimum required age of " + minAge + ".");
        }
        if (maxAge != null && buyerAge > maxAge) {
            return PolicyValidationResult.fail("Buyer age " + buyerAge + " exceeds the maximum allowed age of " + maxAge + ".");
        }
        return PolicyValidationResult.success();
    }
}
