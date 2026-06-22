package com.ticketpurchasingsystem.project.domain.Production.ProductionEvents;

/**
 * Published when a manager/owner appointment request is created (before the
 * appointee has accepted). Listeners notify the appointee; the appointee only
 * becomes an active member once they accept (see AppointManagerEvent /
 * AssignOwnerEvent, fired on acceptance).
 */
public class AppointmentRequestedEvent {
    private final Integer companyId;
    private final String companyName;
    private final String appointeeId;
    private final String appointerId;
    private final String role; // "MANAGER" or "OWNER"

    public AppointmentRequestedEvent(Integer companyId, String companyName, String appointeeId,
            String appointerId, String role) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.appointeeId = appointeeId;
        this.appointerId = appointerId;
        this.role = role;
    }

    public Integer getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public String getAppointeeId() { return appointeeId; }
    public String getAppointerId() { return appointerId; }
    public String getRole() { return role; }
}
