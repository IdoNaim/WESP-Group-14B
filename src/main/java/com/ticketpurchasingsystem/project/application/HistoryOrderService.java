package com.ticketpurchasingsystem.project.application;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public class HistoryOrderService implements IHistoryOrderService {

    private final IHistoryOrderRepo historyOrderRepo;
    private final HistoryOrderHandler historyOrderHandler;
    private final AuthenticationService authenticationService;
    private final ProductionService productionService;

    public HistoryOrderService(IHistoryOrderRepo historyOrderRepo, HistoryOrderHandler historyOrderHandler,
                               AuthenticationService authenticationService,
                               ProductionService productionService) {
        this.historyOrderRepo = historyOrderRepo;
        this.historyOrderHandler = historyOrderHandler;
        this.authenticationService = authenticationService;
        this.productionService = productionService;
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
    public HistoryOrderDTO getHistoryOrder(SessionToken st, String orderId) {
        HistoryOrderDTO historyOrder = null;
        if (isSessionTokenValid(st) && authenticationService.isAdmin(st.getToken())) {
            HistoryOrderItem item = historyOrderRepo.findByOrderId(orderId);
            if (item != null) {
                historyOrder = item.makeDTO();
            }
        }
        return historyOrder;
    }

    @Override
    public List<HistoryOrderDTO> getAllHistoryOrdersByUser(SessionToken st, String userASk) {
        List<HistoryOrderDTO> historyOrders = new java.util.ArrayList<>();
        if (!isSessionTokenValid(st)) return historyOrders;
        String tokenOwner = authenticationService.getUser(st.getToken());
        boolean isOwner = userASk.equals(tokenOwner);
        boolean isAdmin = authenticationService.isAdmin(st.getToken());
        if (!isOwner && !isAdmin) return historyOrders;
        for (HistoryOrderItem item : historyOrderRepo.findAllByUserId(userASk)) {
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
    // This method is intended for system administrators to retrieve all historical orders in the system. It should only be accessible to users with admin privileges, and it will return a list of HistoryOrderDTO objects representing all historical orders.
    public List<HistoryOrderDTO> getAllHistoryOrders(SessionToken st) {
        List<HistoryOrderDTO> historyOrders = new java.util.ArrayList<>();
        if (isSessionTokenValid(st) && authenticationService.isAdmin(st.getToken())) {
            for (HistoryOrderItem item : historyOrderRepo.findAll()) {
                historyOrders.add(item.makeDTO());
            }
        }
        return historyOrders;

    }

    private boolean isSessionTokenValid(SessionToken sessionToken) {
        return authenticationService.validate(sessionToken.getToken());
    }

    private boolean isCompanyInSystem(int companyId) {
        // TODO: Implement a method in productionService to check if a company exists and use it here
        return true; // Placeholder return value
    }

}
