package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy;

import java.util.ArrayList;
import java.util.List;

public class DiscountPolicy {
    private final List<IDiscountRule> discountRules;

    public DiscountPolicy() {
        this.discountRules = new ArrayList<>();
    }

    public void addDiscountRule(IDiscountRule rule) {
        this.discountRules.add(rule);
    }

    public double calculateFinalPrice(double price, DiscountContext context) {
        double finalPrice = price;
        for (IDiscountRule rule : discountRules) {
            finalPrice = rule.applyDiscount(finalPrice, context);
        }
        return finalPrice;
    }
}
