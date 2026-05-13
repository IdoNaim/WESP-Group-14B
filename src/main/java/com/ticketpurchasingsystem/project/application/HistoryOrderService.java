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
    public HistoryOrderDTO getHistoryOrder(SessionToken sessionToken, String orderId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoryOrderDTO> getAllHistoryOrdersByUser(SessionToken sessionToken, String userId) {
        List<HistoryOrderDTO> historyOrders = new java.util.ArrayList<>();
        if(!isSessionTokenValid(sessionToken)) return historyOrders;
        if(!isUserInSystem(userId) || !isAdminInSystem(userId)) return historyOrders;
        for (HistoryOrderItem item : historyOrderRepo.findAllByUserId(userId)) {
            historyOrders.add(item.makeDTO());
        }
        return historyOrders;
    }

    @Override
    public List<HistoryOrderDTO> getAllHistoryOrdersByCompany(SessionToken sessionToken, int companyId) {
        return null;
         // TODO Auto-generated method stub
    }

    @Override
    public List<HistoryOrderDTO> getAllHistoryOrders(SessionToken sessionToken) {
        return null;
         // TODO Auto-generated method stub
    }

    private boolean isSessionTokenValid(SessionToken sessionToken) {
        return authenticationService.validate(sessionToken.getToken());
    }

    private boolean isUserInSystem(String userId) {
        return userService.getAllUsers().stream().anyMatch(user -> user.getUserId().equals(userId));
    }

    private boolean isAdminInSystem(String userId) {
        return systemAdminService.getAllUsers().stream().anyMatch(admin -> admin.getId().equals(String.valueOf(userId)));
    }

    private boolean isCompanyInSystem(int companyId) {
        // TODO: Implement a method in productionService to check if a company exists and use it here
        return true; // Placeholder return value
    }

}
