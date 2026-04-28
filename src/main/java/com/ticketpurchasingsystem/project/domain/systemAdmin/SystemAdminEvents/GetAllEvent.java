package com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents;

import java.util.List;

public abstract class GetAllEvent<T> {
    private final String reqId;
    private List<T> result;

    protected GetAllEvent(String reqId) {
        this.reqId = reqId;
    }

    public String getReqId() {
        return reqId;
    }

    public List<T> getResult() {
        return result;
    }

    public void setResult(List<T> result) {
        this.result = result;
    }
}