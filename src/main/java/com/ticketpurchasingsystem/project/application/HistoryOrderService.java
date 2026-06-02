package com.ticketpurchasingsystem.project.application;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderHandler;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

import jakarta.annotation.PostConstruct;

@Service
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
        if (isSessionTokenValid(st)){
            loggerDef.getInstance().info("Getting history order with ID: " + orderId + " for user: " + authenticationService.getUser(st.getToken()));
            HistoryOrderItem item = historyOrderRepo.findByOrderId(orderId);
            if (item != null) {
                loggerDef.getInstance().info("Found history order: " + item.getOrderId() + " for user: " + item.getUserId());
                if (authenticationService.isAdmin(st.getToken()) || item.getUserId().equals(authenticationService.getUser(st.getToken()))) {
                    historyOrder = item.makeDTO();
                    loggerDef.getInstance().info("Returning history order: " + historyOrder.getOrderId() + " for user: " + historyOrder.getUserId());
                }
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
        List<HistoryOrderDTO> historyOrders = new java.util.ArrayList<>();
        if(isSessionTokenValid(sessionToken)){
            for (HistoryOrderItem item : historyOrderRepo.findAllByCompanyId(companyId)) {
                historyOrders.add(item.makeDTO());
            }
        }
        return historyOrders;
    }
    
    @Override
    // This method is intended for system administrators to retrieve all historical orders in the system. It should only be accessible to users with admin privileges, and it will return a list of HistoryOrderDTO objects representing all historical orders.
    public List<HistoryOrderDTO> getAllHistoryOrders(SessionToken st) {
        if (!isSessionTokenValid(st)) {
            throw new RuntimeException("Session has ended");
        }
        if (!authenticationService.isAdmin(st.getToken())) {
            throw new SecurityException("Admin access required");
        }
        List<HistoryOrderDTO> historyOrders = new java.util.ArrayList<>();
        for (HistoryOrderItem item : historyOrderRepo.findAll()) {
            historyOrders.add(item.makeDTO());
        }
        return historyOrders;
    }

    private boolean isSessionTokenValid(SessionToken sessionToken) {
        return authenticationService.validate(sessionToken.getToken());
    }
}
