package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy;

public interface IPurchaseRule {
    boolean validate(PurchaseContext context);
}
