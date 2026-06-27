package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

public class OrRule implements IPurchaseRule {
    private final IPurchaseRule component1;
    private final IPurchaseRule component2;

    public OrRule(IPurchaseRule component1, IPurchaseRule component2) {
        this.component1 = component1;
        this.component2 = component2;
    }

    @Override
    public boolean validate(PurchaseContext context) {
        int total = context.getAlreadyPurchased() + context.getRequestedAmount();
        if (component1 instanceof MinTicketsRule && total >= ((MinTicketsRule) component1).getLimit()) {
            return true;
        }
        if (component2 instanceof MinTicketsRule && total >= ((MinTicketsRule) component2).getLimit()) {
            return true;
        }
        return component1.validate(context) || component2.validate(context);
    }
    public IPurchaseRule getComponent1() {
        return component1;
    }
    public IPurchaseRule getComponent2() {
        return component2;
    }
    public boolean validateTicketPolicy(PurchaseContext context){
        int total = context.getAlreadyPurchased() + context.getRequestedAmount();
        if (component1 instanceof MinTicketsRule && total >= ((MinTicketsRule) component1).getLimit()) {
            return true;
        }
        if (component2 instanceof MinTicketsRule && total >= ((MinTicketsRule) component2).getLimit()) {
            return true;
        }
        return component1.validateTicketPolicy(context) || component2.validateTicketPolicy(context);
    }
}
