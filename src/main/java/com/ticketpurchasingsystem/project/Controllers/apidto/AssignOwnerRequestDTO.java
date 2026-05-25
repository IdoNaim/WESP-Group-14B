package com.ticketpurchasingsystem.project.Controllers.apidto;

public class AssignOwnerRequestDTO {
    private String appointeeUserId;

    public AssignOwnerRequestDTO() {}

    public String getAppointeeUserId() {
        return appointeeUserId;
    }

    public void setAppointeeUserId(String appointeeUserId) {
        this.appointeeUserId = appointeeUserId;
    }
}
