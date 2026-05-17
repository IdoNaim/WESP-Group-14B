
package com.ticketpurchasingsystem.project.application;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.systemAdmin.IAdminRepo;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdmin;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;;


public class SystemAdminService implements ISystemAdminService {
    private final IAdminRepo adminRepo;
    private final SystemAdmin systemAdmin;
    private final loggerDef logger = loggerDef.getInstance();

    public SystemAdminService(IAdminRepo adminRepo ,SystemAdmin systemAdmin) {
         this.systemAdmin = systemAdmin;
        this.adminRepo = adminRepo;
    }
    @Override
    public List<ActiveOrderItem> getAllActiveOrders() {
        logger.info("SystemAdminService: Fetching all active orders");
        return systemAdmin.getAllActiveOrders();
    }
    @Override
    public List<HistoryOrderItem> getAllHistoryOrders() {
        logger.info("SystemAdminService: Fetching all history orders");
        return systemAdmin.getAllHistoryOrderItems();
    }
}