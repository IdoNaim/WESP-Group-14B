package com.ticketpurchasingsystem.project.domain.tickets;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

import java.util.ArrayList;
import java.util.List;

public class PurchaseRuleAdapter implements ITicketPurchaseRule {
    private final IPurchaseRule targetRule;
    private final String type;
    private final Integer minAge;
    private final Integer maxAge;
    private final Integer minTickets;
    private final Integer maxTickets;
    private final List<ITicketPurchaseRule> subRules;

    public PurchaseRuleAdapter(IPurchaseRule targetRule, String type, Integer minAge, Integer maxAge, Integer minTickets, Integer maxTickets, List<ITicketPurchaseRule> subRules) {
        this.targetRule = targetRule;
        this.type = type;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.minTickets = minTickets;
        this.maxTickets = maxTickets;
        this.subRules = subRules != null ? subRules : new ArrayList<>();
    }

    public IPurchaseRule getTargetRule() {
        return targetRule;
    }

    public String getType() {
        return type;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public Integer getMinTickets() {
        return minTickets;
    }

    public Integer getMaxTickets() {
        return maxTickets;
    }

    public List<ITicketPurchaseRule> getSubRules() {
        return subRules;
    }

    @Override
    public PolicyValidationResult validate(TicketPurchaseContext context) {
        // Construct the teammate's context
        PurchaseContext teammateContext = new PurchaseContext(context.getRequestedTickets(), context.getBuyerAge());
        
        // Delegate validation to the teammate's rule engine
        boolean valid = targetRule.validate(teammateContext);
        if (valid) {
            return PolicyValidationResult.success();
        }

        // If validation fails, determine the specific reason
        return PolicyValidationResult.fail(getRejectionMessage(context));
    }

    private String getRejectionMessage(TicketPurchaseContext context) {
        int buyerAge = context.getBuyerAge();
        int requestedTickets = context.getRequestedTickets();

        switch (type.toUpperCase()) {
            case "AGE":
                if (minAge != null && buyerAge < minAge) {
                    return "Buyer age " + buyerAge + " is less than the minimum required age of " + minAge + ".";
                }
                if (maxAge != null && buyerAge > maxAge) {
                    return "Buyer age " + buyerAge + " exceeds the maximum allowed age of " + maxAge + ".";
                }
                return "Buyer age violates age policy restrictions.";
            case "MIN_TICKETS":
                return "Requested tickets " + requestedTickets + " is less than the minimum limit of " + minTickets + ".";
            case "MAX_TICKETS":
                return "Requested tickets " + requestedTickets + " exceeds the maximum limit of " + maxTickets + ".";
            case "AND":
                // In AND composition, return the rejection message of the first failing sub-rule
                for (ITicketPurchaseRule subRule : subRules) {
                    PolicyValidationResult result = subRule.validate(context);
                    if (!result.isValid()) {
                        return result.getRejectionMessage();
                    }
                }
                return "AND policy validation failed.";
            case "OR":
                // In OR composition, combine rejection messages of all sub-rules
                List<String> messages = new ArrayList<>();
                for (ITicketPurchaseRule subRule : subRules) {
                    PolicyValidationResult result = subRule.validate(context);
                    if (!result.isValid() && result.getRejectionMessage() != null) {
                        messages.add(result.getRejectionMessage());
                    }
                }
                return String.join(" OR ", messages);
            default:
                return "Purchase policy validation failed.";
        }
    }
}
