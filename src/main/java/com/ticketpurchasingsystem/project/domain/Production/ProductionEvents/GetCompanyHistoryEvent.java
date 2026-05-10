package com.ticketpurchasingsystem.project.domain.Production.ProductionEvents;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;

public class GetCompanyHistoryEvent {
    private final int companyId;
    private List<HistoryOrderItem> result;

    public GetCompanyHistoryEvent(int companyId) {
        this.companyId = companyId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public List<HistoryOrderItem> getResult() {
        return result;
    }

    public void setResult(List<HistoryOrderItem> result) {
        this.result = result;
    }
}
