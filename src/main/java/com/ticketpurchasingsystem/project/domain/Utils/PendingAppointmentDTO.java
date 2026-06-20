package com.ticketpurchasingsystem.project.domain.Utils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;

/**
 * A not-yet-answered appointment request shown to the appointee: which company
 * invited them, the role they would take, who appointed them, and (for manager
 * invites) the permissions attached.
 */
public class PendingAppointmentDTO {
    private final Integer companyId;
    private final String companyName;
    private final String role;
    private final String appointerId;
    private final Set<ManagerPermission> permissions;

    public PendingAppointmentDTO(Integer companyId, String companyName, String role,
            String appointerId, Set<ManagerPermission> permissions) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.role = role;
        this.appointerId = appointerId;
        this.permissions = (permissions == null || permissions.isEmpty())
                ? Collections.emptySet()
                : EnumSet.copyOf(permissions);
    }

    public Integer getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public String getRole() { return role; }
    public String getAppointerId() { return appointerId; }
    public Set<ManagerPermission> getPermissions() { return Collections.unmodifiableSet(permissions); }
}
