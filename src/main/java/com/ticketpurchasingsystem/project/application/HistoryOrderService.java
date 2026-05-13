package com.ticketpurchasingsystem.project.application;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.sql.Timestamp;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.IHistoryOrderService;
import com.ticketpurchasingsystem.project.application.ISystemAdminService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.*;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;

public class HistoryOrderService implements IHistoryOrderService {

    private final IHistoryOrderRepo historyOrderRepo;
    private final HistoryOrderHandler historyOrderHandler;
    private final AuthenticationService authenticationService;
    private final ISystemAdminService systemAdminService;
    private final ProductionService productionService;
    private final IUserService userService;

    public HistoryOrderService(IHistoryOrderRepo historyOrderRepo, HistoryOrderHandler historyOrderHandler, AuthenticationService authenticationService, ISystemAdminService systemAdminService, ProductionService productionService, IUserService userService) {
        this.historyOrderRepo = historyOrderRepo;
        this.historyOrderHandler = historyOrderHandler;
        this.authenticationService = authenticationService;
        this.systemAdminService = systemAdminService;
        this.productionService = productionService;
        this.userService = userService;
    }


    @Override
    public boolean createHistoryOrder(String sessionToken, String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities) {
        if (!authenticationService.validate(sessionToken)) {
            return false;
        }
        HistoryOrderItem newHistoryOrder = historyOrderHandler.saveHistoryOrder(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, standingAreaQuantities);
        if (newHistoryOrder != null) {
            historyOrderRepo.save(newHistoryOrder);
            return true;
        }
        return false;
    }

    @Override
    public void getHistoryOrder(SessionToken sessionToken, String orderId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getAllHistoryOrdersByUser(SessionToken sessionToken, String userId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getAllHistoryOrdersByCompany(SessionToken sessionToken, int companyId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getAllHistoryOrders(SessionToken sessionToken) {
        // TODO Auto-generated method stub
    }  
}
