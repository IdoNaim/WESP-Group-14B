package com.ticketpurchasingsystem.project.domain.Utils;

public class OwnerDTO {
    private final String userId;
    private final String appointerId;

    public OwnerDTO(String userId, String appointerId) {
        this.userId = userId;
        this.appointerId = appointerId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAppointerId() {
        return appointerId;
    }

    public boolean isFounder() {
        return appointerId == null;
    }

    @Override
    public String toString() {
        return "OwnerDTO{userId='" + userId + "', appointerId='" + appointerId + "'}";
    }
}
