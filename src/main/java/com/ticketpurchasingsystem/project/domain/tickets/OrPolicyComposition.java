package com.ticketpurchasingsystem.project.domain.tickets;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OrPolicyComposition implements ITicketPurchaseRule {
    private final List<ITicketPurchaseRule> rules;

    public OrPolicyComposition(List<ITicketPurchaseRule> rules) {
        this.rules = new ArrayList<>(rules != null ? rules : new ArrayList<>());
    }

    public List<ITicketPurchaseRule> getRules() {
        return new ArrayList<>(rules);
    }

    @Override
    public PolicyValidationResult validate(TicketPurchaseContext context) {
        if (rules.isEmpty()) {
            return PolicyValidationResult.success();
        }

        Set<String> uniqueMessages = new LinkedHashSet<>();
        for (ITicketPurchaseRule rule : rules) {
            PolicyValidationResult result = rule.validate(context);
            if (result.isValid()) {
                return PolicyValidationResult.success();
            }
            if (result.getRejectionMessage() != null) {
                uniqueMessages.add(result.getRejectionMessage());
            }
        }

        String combinedMessage = String.join(" OR ", uniqueMessages);
        return PolicyValidationResult.fail(combinedMessage);
    }
}
