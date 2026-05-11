package com.ticketpurchasingsystem.project.domain.Production;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AppointManagerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.ModifyManagerPermissionsEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Component
public class ProdListener {

    @EventListener
    public void onNewProductionCompany(NewProdEvent event) {
        String companyName = event.getCompany().getCompanyName();
        String founderId = event.getCompany().getFounderId();
        loggerDef.getInstance().info(
                "New Production Company created: Name=" + companyName + ", FounderID=" + founderId);
    }

    @EventListener
    public void onAssignOwner(AssignOwnerEvent event) {
        String companyName = event.getCompany().getCompanyName();
        String appointeeId = event.getAppointeeId();
        String appointerId = event.getAppointerId();
        loggerDef.getInstance().info(
                "Owner assigned: Company=" + companyName
                        + ", NewOwner=" + appointeeId
                        + ", AppointedBy=" + appointerId);
    }

    @EventListener
    public void onModifyManagerPermissions(ModifyManagerPermissionsEvent event) {
        loggerDef.getInstance().info(
                "Manager permissions updated: Company=" + event.getCompany().getCompanyName()
                        + ", Manager=" + event.getManagerId()
                        + ", UpdatedBy=" + event.getOwnerId()
    @EventListener
    public void onAppointManager(AppointManagerEvent event) {
        String companyName = event.getCompany().getCompanyName();
        String managerId = event.getManagerId();
        String appointerId = event.getAppointerId();
        loggerDef.getInstance().info(
                "Manager appointed: Company=" + companyName
                        + ", NewManager=" + managerId
                        + ", AppointedBy=" + appointerId
                        + ", Permissions=" + event.getPermissions());
    }
}
