package com.ticketpurchasingsystem.project.application;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

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


    public boolean createHistoryOrder(String orderId, String userId, String eventId, int companyId, Timestamp purchaseDate, double price, List<String> seatIds, HashMap<String, Integer> standingAreaQuantities) {
        HistoryOrderDTO historyOrderDTO = new HistoryOrderDTO(orderId, userId, eventId, companyId, purchaseDate, price, seatIds, standingAreaQuantities);
        HistoryOrderItem newHistoryOrder = historyOrderHandler.saveHistoryOrder(historyOrderDTO);
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
