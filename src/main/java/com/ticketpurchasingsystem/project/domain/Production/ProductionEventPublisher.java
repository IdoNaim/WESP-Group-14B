package com.ticketpurchasingsystem.project.domain.Production;

import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.GetCompanyHistoryEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.ModifyManagerPermissionsEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;

public class ProductionEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public ProductionEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public boolean publishIsUserRegisteredEvent(String userId) {
        IsUserRegisteredEvent event = new IsUserRegisteredEvent(userId);
        eventPublisher.publishEvent(event);
        return event.isRegistered();
    }

    public List<HistoryOrderItem> publishGetCompanyHistoryEvent(int companyId) {
        GetCompanyHistoryEvent event = new GetCompanyHistoryEvent(companyId);
        eventPublisher.publishEvent(event);
        return event.getResult();
    }

    public void publishNewProdEvent(ProductionCompany company) {
        eventPublisher.publishEvent(new NewProdEvent(company));
    }

    public void publishAssignOwnerEvent(ProductionCompany company, String appointerId, String appointeeId) {
        eventPublisher.publishEvent(new AssignOwnerEvent(company, appointerId, appointeeId));
    }

    public void publishModifyManagerPermissionsEvent(ProductionCompany company, String ownerId,
            String managerId, Set<ManagerPermission> permissions) {
        eventPublisher.publishEvent(new ModifyManagerPermissionsEvent(company, ownerId, managerId, permissions));
    }
}
