package com.ticketpurchasingsystem.project.domain.Production.ProductionEvents;

import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;

public class AssignOwnerEvent {
    private final ProductionCompany company;
    private final String appointerId;
    private final String appointeeId;

    public AssignOwnerEvent(ProductionCompany company, String appointerId, String appointeeId) {
        this.company = company;
        this.appointerId = appointerId;
        this.appointeeId = appointeeId;
    }

    public ProductionCompany getCompany() {
        return company;
    }

    public String getAppointerId() {
        return appointerId;
    }

    public String getAppointeeId() {
        return appointeeId;
    }
}