package com.ticketpurchasingsystem.project.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;

@Repository
public class HistoryOrderRepo implements IHistoryOrderRepo {

    private final ConcurrentHashMap<String, HistoryOrderItem> storage = new ConcurrentHashMap<>();

    private static HistoryOrderRepo instance;

    public static HistoryOrderRepo getInstance() {
        if (instance == null) {
            instance = new HistoryOrderRepo();
        }
        return instance;
    }

    @Override
    public HistoryOrderItem save(HistoryOrderItem historyOrder) {
        storage.put(historyOrder.getOrderId(), historyOrder);
        return historyOrder;
    }

    @Override
    public List<HistoryOrderItem> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<HistoryOrderItem> findAllByCompanyId(int companyId) {
        List<HistoryOrderItem> result = new ArrayList<>();
        for (HistoryOrderItem item : storage.values()) {
            if (item.getCompanyId() == companyId) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<HistoryOrderItem> findAllByUserId(String userId) {
        List<HistoryOrderItem> result = new ArrayList<>();
        for (HistoryOrderItem item : storage.values()) {
            if (item.getUserId().equals(userId)) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<HistoryOrderItem> findAllByEventId(String eventId) {
        List<HistoryOrderItem> result = new ArrayList<>();
        for (HistoryOrderItem item : storage.values()) {
            if (item.getEventId().equals(eventId)) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }

    @Override
    public HistoryOrderItem findByOrderId(String orderId) {
        HistoryOrderItem item = null;
        for (HistoryOrderItem historyOrder : storage.values()) {
            if (historyOrder.getOrderId().equals(orderId)) {
                item = historyOrder;
                break;
            }
        }
        return item;
    }
}
