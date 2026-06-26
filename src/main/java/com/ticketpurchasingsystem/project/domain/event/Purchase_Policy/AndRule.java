package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

public class AndRule implements IPurchaseRule{
    private final IPurchaseRule component1;
    private final IPurchaseRule component2;
    public AndRule(IPurchaseRule rule1, IPurchaseRule rule2) {
        this.component1 = rule1;
        this.component2 = rule2;
    }
    @Override
    public boolean validate(PurchaseContext context) {
        return component1.validate(context) && component2.validate(context);
    }
    public IPurchaseRule getComponent1() {
        return component1;
    }
    public IPurchaseRule getComponent2() {
        return component2;
    }
    public boolean validateTicketPolicy(PurchaseContext context){
        return component1.validateTicketPolicy(context) && component2.validateTicketPolicy(context);
    }
}
