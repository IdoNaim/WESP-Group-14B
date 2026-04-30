package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy;

import java.util.ArrayList;
import java.util.List;

public class PurchasePolicy {
    private final List<IPurchaseRule> rules;

    public PurchasePolicy() {
        this.rules = new ArrayList<>();
    }

    public void addRule(IPurchaseRule rule) {
        rules.add(rule);
    }

    public boolean validate(PurchaseContext context) {
        for (IPurchaseRule rule : rules) {
            if (!rule.validate(context)) {
                return false;
            }
        }
        return true;
    }
}
