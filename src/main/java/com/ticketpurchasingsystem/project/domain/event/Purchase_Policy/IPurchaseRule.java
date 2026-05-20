package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

public interface IPurchaseRule {
    boolean validate(PurchaseContext context);
}
