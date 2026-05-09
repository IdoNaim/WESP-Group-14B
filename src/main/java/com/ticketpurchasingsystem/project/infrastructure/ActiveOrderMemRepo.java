package com.ticketpurchasingsystem.project.infrastructure;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ActiveOrderMemRepo implements IActiveOrderRepo {

    private final ConcurrentHashMap<String, ActiveOrderItem> activeOrders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> orderLocks = new ConcurrentHashMap<>();
//    @Override
//    public boolean save(ActiveOrderItem order) {
//        activeOrders.add(order);
//        return true;
//    }
    private ReentrantLock getLockFor(String orderId) {
        return orderLocks.computeIfAbsent(orderId, id -> new ReentrantLock());
    }

    @Override
    public boolean save(ActiveOrderItem order) {
        ReentrantLock lock = getLockFor(order.getOrderId());
        lock.lock();
        try {
            activeOrders.put(order.getOrderId(), order);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ActiveOrderItem findById(String orderId) {
        ReentrantLock lock = orderLocks.get(orderId);
        if (lock == null) {
            return null;
        }
        lock.lock();
        try {
            ActiveOrderItem order = activeOrders.get(orderId);
            if(order != null) {
                return new ActiveOrderItem(order);
            }
            else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String orderId) {
        ReentrantLock lock = orderLocks.get(orderId);
        if (lock == null) {
            return;
        }
        lock.lock();
        try {
            activeOrders.remove(orderId);
            orderLocks.remove(orderId); // clean up the lock too
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void update(ActiveOrderItem order) {
        ReentrantLock lock = getLockFor(order.getOrderId());
        lock.lock();
        try {
            ActiveOrderItem existing = activeOrders.get(order.getOrderId());
            if (existing != null) {
                existing.editOrder(order);
            } else {
                activeOrders.put(order.getOrderId(), order); // upsert
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<ActiveOrderItem> findAll() {
        return List.copyOf(activeOrders.values());
    }
}

