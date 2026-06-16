package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

import java.util.Arrays;
import java.util.List;

public class AndRule implements IPurchaseRule {
    private final List<IPurchaseRule> rules;

    public AndRule(IPurchaseRule... rules) {
        this.rules = Arrays.asList(rules);
    }

    public List<IPurchaseRule> getRules() { return rules; }

    @Override
    public boolean validate(PurchaseContext context) {
        for (IPurchaseRule rule : rules) {
            if (!rule.validate(context)) {
                return false;
            }
        }
        return true;
    }
}
