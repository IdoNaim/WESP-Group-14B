package com.ticketpurchasingsystem.project.application;

import java.util.List;
import java.util.Set;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;

public interface IProductionService {
    public void createEvent(String eventName, String eventDate, String eventLocation, int totalTickets, String userId);

    public void updateEvent(String eventId, String eventName, String eventDate, String eventLocation, int totalTickets,
            String userId);

    public void deleteEvent(String eventId, String userId);

    public String getEventAsManager(String eventId, String userId);

    public String getAllEventsAsManager(String userId);

    public String getEventAsCustomer(String eventId);

    public boolean createProductionCompany(String sessionToken,
            ProductionCompanyDTO companyDetails);

    public boolean assignOwner(String sessionToken, Integer companyId, String appointeeUserId);

    public boolean appointManager(String sessionToken, Integer companyId, String managerId,
            Set<ManagerPermission> permissions);

    public List<HistoryOrderItem> getCompanyPurchaseHistory(String sessionToken, Integer companyId);

    public boolean modifyManagerPermissions(String sessionToken, Integer companyId,
            String managerId, Set<ManagerPermission> permissions);
    public boolean removeManager(String sessionToken, Integer companyId, String managerId);
    public RolesTreeDTO getRolesTree(String sessionToken, Integer companyId);
}
