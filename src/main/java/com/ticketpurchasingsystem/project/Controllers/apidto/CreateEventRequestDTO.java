package com.ticketpurchasingsystem.project.Controllers.apidto;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;

public class CreateEventRequestDTO {

    private EventDTO event;
    private PurchasePolicyDTO purchasePolicy;
    private List<DiscountDTO> discounts;
    private String sessionToken;

    public CreateEventRequestDTO() {}

    public String getSessionToken() {
        return sessionToken;
    }
    public EventDTO getEvent() { return event; }
    public void setEvent(EventDTO event) { this.event = event; }

    public PurchasePolicyDTO getPurchasePolicy() { return purchasePolicy; }
    public void setPurchasePolicy(PurchasePolicyDTO purchasePolicy) { this.purchasePolicy = purchasePolicy; }

    public List<DiscountDTO> getDiscounts() { return discounts; }
    public void setDiscounts(List<DiscountDTO> discounts) { this.discounts = discounts; }
}
