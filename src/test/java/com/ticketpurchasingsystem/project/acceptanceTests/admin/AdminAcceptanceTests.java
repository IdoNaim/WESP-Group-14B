package com.ticketpurchasingsystem.project.acceptanceTests.admin;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderListener;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.systemAdmin.AdminInfo;
import com.ticketpurchasingsystem.project.domain.systemAdmin.AdminPublisher;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdmin;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllActiveOrdersEvent;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllHistoryOrdersEvent;
import com.ticketpurchasingsystem.project.infrastructure.ActiveOrderMemRepo;
import com.ticketpurchasingsystem.project.infrastructure.HistoryOrderRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminAcceptanceTests {

    private ActiveOrderMemRepo activeOrderRepo;
    private HistoryOrderRepo historyOrderRepo;
    private SystemAdmin systemAdmin;

    @BeforeEach
    void setUp() {
        activeOrderRepo = new ActiveOrderMemRepo();
        historyOrderRepo = new HistoryOrderRepo();

        ActiveOrderListener activeOrderListener = new ActiveOrderListener(activeOrderRepo);

        // manual synchronous event bus — routes admin events to the real listeners
        AdminPublisher adminPublisher = new AdminPublisher(event -> {
            if (event instanceof GetAllActiveOrdersEvent e)
                activeOrderListener.handleGetAllActiveOrdersEvent(e);
            else if (event instanceof GetAllHistoryOrdersEvent e)
                e.setResult(historyOrderRepo.findAll());
        });

        systemAdmin = new SystemAdmin(new AdminInfo("sysadmin", "admin@system.com"), adminPublisher);
    }

    // ─── getAllActiveOrders ───────────────────────────────────────────────────

    @Test
    void GivenNoActiveOrders_WhenGetAllActiveOrders_ThenReturnEmptyList() {
        List<ActiveOrderItem> result = systemAdmin.getAllActiveOrders();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void GivenActiveOrdersExist_WhenGetAllActiveOrders_ThenAllOrdersAreReturned() {
        activeOrderRepo.save(new ActiveOrderItem("order-1", "user-1", "event-1"));
        activeOrderRepo.save(new ActiveOrderItem("order-2", "user-2", "event-1"));

        List<ActiveOrderItem> result = systemAdmin.getAllActiveOrders();

        assertEquals(2, result.size());
    }

    @Test
    void GivenAnActiveOrder_WhenGetAllActiveOrders_ThenThatOrderIsIncluded() {
        activeOrderRepo.save(new ActiveOrderItem("order-1", "user-1", "event-1"));

        List<ActiveOrderItem> result = systemAdmin.getAllActiveOrders();

        assertTrue(result.stream().anyMatch(o -> "order-1".equals(o.getOrderId())));
    }

    // ─── getAllHistoryOrderItems ──────────────────────────────────────────────

    @Test
    void GivenNoHistoryOrders_WhenGetAllHistoryOrders_ThenReturnEmptyList() {
        List<HistoryOrderItem> result = systemAdmin.getAllHistoryOrderItems();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void GivenHistoryOrdersExist_WhenGetAllHistoryOrders_ThenAllOrdersAreReturned() {
        historyOrderRepo.save(new HistoryOrderItem("h-1", "user-1", "event-1", 1, 50.0, List.of(), new HashMap<>()));
        historyOrderRepo.save(new HistoryOrderItem("h-2", "user-2", "event-1", 1, 75.0, List.of(), new HashMap<>()));

        List<HistoryOrderItem> result = systemAdmin.getAllHistoryOrderItems();

        assertEquals(2, result.size());
    }

    @Test
    void GivenAHistoryOrder_WhenGetAllHistoryOrders_ThenThatOrderIsIncluded() {
        historyOrderRepo.save(new HistoryOrderItem("h-1", "user-1", "event-1", 1, 50.0, List.of(), new HashMap<>()));

        List<HistoryOrderItem> result = systemAdmin.getAllHistoryOrderItems();

        assertTrue(result.stream().anyMatch(o -> "h-1".equals(o.getOrderId())));
    }
}
