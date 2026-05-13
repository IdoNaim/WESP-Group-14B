package com.ticketpurchasingsystem.project.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

public class HistoryOrderRepo implements IHistoryOrderRepo {

    private final ConcurrentHashMap<Integer, HistoryOrderItem> storage = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private static HistoryOrderRepo instance;

    public static HistoryOrderRepo getInstance() {
        if (instance == null) {
            instance = new HistoryOrderRepo();
        }
        return instance;
    }

    @Override
    public void save(HistoryOrderItem historyOrder) {
        int id = idGenerator.getAndIncrement();
        storage.put(id, historyOrder);
    }

    @Override
    public List<HistoryOrderItem> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<HistoryOrderItem> findByCompanyId(int companyId) {
        List<HistoryOrderItem> result = new ArrayList<>();
        for (HistoryOrderItem item : storage.values()) {
            if (item.getCompanyId() == companyId) {
                result.add(item);
            }
        }
        return result;
    }
}
