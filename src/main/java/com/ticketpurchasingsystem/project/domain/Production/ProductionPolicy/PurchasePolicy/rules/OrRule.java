package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;

import java.util.Arrays;
import java.util.List;

public class OrRule implements IPurchaseRule {
    private final List<IPurchaseRule> rules;

    public OrRule(IPurchaseRule... rules) {
        this.rules = Arrays.asList(rules);
    }

    @Override
    public boolean validate(PurchaseContext context) {
        for (IPurchaseRule rule : rules) {
            if (rule.validate(context)) {
                return true;
            }
        }
        return false;
    }
}
