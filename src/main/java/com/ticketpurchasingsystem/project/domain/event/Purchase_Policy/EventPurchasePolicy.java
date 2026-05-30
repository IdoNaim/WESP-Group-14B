package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

import java.util.ArrayList;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;

/**
 * EventPurchasePolicy is a Composite of IPurchaseRules.
 * It evaluates to true only if ALL of its child rules evaluate to true.
 */
public class EventPurchasePolicy implements IPurchaseRule {

    private final List<IPurchaseRule> rules;

    public EventPurchasePolicy() {
        this.rules = new ArrayList<>();
    }

    // Allows dynamic addition of rules
    public void addRule(IPurchaseRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    // Allows dynamic removal of rules if the policy changes
    public void removeRule(IPurchaseRule rule) {
        rules.remove(rule);
    }

    @Override
    public boolean validate(PurchaseContext context) {
        for (IPurchaseRule rule : rules) {
            if (!rule.validate(context)) {
                return false; // Fails fast if any rule is violated
            }
        }
        return true; // Passes if all rules pass (or if there are no rules)
    }
    public PurchasePolicyDTO getDTO() {
        //TODO: Implement this method to convert the policy to a DTO for API responses
        return null; // Implement this method to convert the policy to a DTO for API responses
    }
}