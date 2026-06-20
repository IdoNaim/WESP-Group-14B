package com.ticketpurchasingsystem.project.application;

import java.util.List;
import java.util.Set;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Utils.CompanySummaryDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;
import com.ticketpurchasingsystem.project.domain.Utils.MemberInfoDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PendingAppointmentDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;

public interface IProductionService {
        public void createEvent(String eventName, String eventDate, String eventLocation, int totalTickets,
                        String userId);

        public void updateEvent(String eventId, String eventName, String eventDate, String eventLocation,
                        int totalTickets,
                        String userId);

        public void deleteEvent(String eventId, String userId);

        public String getEventAsManager(String eventId, String userId);

        public String getAllEventsAsManager(String userId);

        public String getEventAsCustomer(String eventId);

        public Integer createProductionCompany(String sessionToken,
                        ProductionCompanyDTO companyDetails);

        public boolean assignOwner(String sessionToken, Integer companyId, String appointeeUserId);

        public boolean appointManager(String sessionToken, Integer companyId, String managerId,
                        Set<ManagerPermission> permissions);

        public List<HistoryOrderItem> getCompanyPurchaseHistory(String sessionToken, Integer companyId);

        public boolean modifyManagerPermissions(String sessionToken, Integer companyId,
                        String managerId, Set<ManagerPermission> permissions);

        public boolean removeManager(String sessionToken, Integer companyId, String managerId);

        public boolean removeOwner(String sessionToken, Integer companyId, String ownerId);

        public RolesTreeDTO getRolesTree(String sessionToken, Integer companyId);

        public boolean addPurchasePolicyRule(String sessionToken, Integer companyId, IPurchaseRule rule);

        public boolean setCompanyPurchasePolicy(String sessionToken, Integer companyId, PurchasePolicyDTO dto);

        public PurchasePolicyDTO getCompanyPurchasePolicy(String sessionToken, Integer companyId);

        public List<CompanySummaryDTO> getMyCompanies(String sessionToken);

        public MemberInfoDTO getMyMemberInfo(String sessionToken, Integer companyId);

        public List<PendingAppointmentDTO> getMyPendingAppointments(String sessionToken);

        public boolean acceptAppointment(String sessionToken, Integer companyId);

        public boolean denyAppointment(String sessionToken, Integer companyId);
}
