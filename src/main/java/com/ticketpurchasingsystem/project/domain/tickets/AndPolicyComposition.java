package com.ticketpurchasingsystem.project.domain.tickets;

import java.util.ArrayList;
import java.util.List;

public class AndPolicyComposition implements ITicketPurchaseRule {
    private final List<ITicketPurchaseRule> rules;

    public AndPolicyComposition(List<ITicketPurchaseRule> rules) {
        this.rules = new ArrayList<>(rules != null ? rules : new ArrayList<>());
    }

    public List<ITicketPurchaseRule> getRules() {
        return new ArrayList<>(rules);
    }

    @Override
    public PolicyValidationResult validate(TicketPurchaseContext context) {
        for (ITicketPurchaseRule rule : rules) {
            PolicyValidationResult result = rule.validate(context);
            if (!result.isValid()) {
                return result; // Short-circuit and return the first failure
            }
        }
        return PolicyValidationResult.success();
    }
}
