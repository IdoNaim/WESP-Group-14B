package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy;

public interface IDiscountRule {
    double applyDiscount(double currentPrice, DiscountContext context);
}
