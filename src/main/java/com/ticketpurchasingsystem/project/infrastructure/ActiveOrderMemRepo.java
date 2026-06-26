package com.ticketpurchasingsystem.project.infrastructure;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ActiveOrderMemRepo implements IActiveOrderRepo {


    private final ConcurrentHashMap<String, ActiveOrderItem> activeOrders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> orderLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userToOrder = new ConcurrentHashMap<>();
    private ReentrantLock getLockFor(String orderId) {
        return orderLocks.computeIfAbsent(orderId, id -> new ReentrantLock());
    }

    @Override
    public ActiveOrderItem save(ActiveOrderItem order) {
        if(order == null){
            throw new IllegalArgumentException("tried to save null active order");
        }

        // 💡 FIX FOR TEST SUITE NPEs: Simulate Hibernate's @GeneratedValue UUID behavior
        if (order.getOrderId() == null) {
            try {
                java.lang.reflect.Field idField = order.getClass().getDeclaredField("orderId");
                idField.setAccessible(true);
                idField.set(order, java.util.UUID.randomUUID().toString());
            } catch (Exception e) {
                // Note: If ActiveOrderItem has a standard setter, you can replace the
                // reflection block above with a simple: order.setOrderId(java.util.UUID.randomUUID().toString());
                throw new RuntimeException("Failed to inject simulated UUID for test environment", e);
            }
        }

        // This will now execute perfectly without NPE because order.getOrderId() is a valid string!
        String existing = userToOrder.putIfAbsent(order.getUserId(), order.getOrderId());
        if (existing != null) {
            throw new IllegalArgumentException("the user "+ order.getUserId()+ " already has an active order");
        }

        activeOrders.put(order.getOrderId(), order);
        getLockFor(order.getOrderId());
        return order;
    }

    @Override
    public Optional<ActiveOrderItem> findById(String orderId) {
        ReentrantLock lock = orderLocks.get(orderId);
        if (lock == null) {
            return Optional.empty();
        }
        lock.lock();
        try {
            ActiveOrderItem order = activeOrders.get(orderId);
            return order != null ? Optional.of(new ActiveOrderItem(order)) : Optional.empty();
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
            ActiveOrderItem order = activeOrders.get(orderId);
            if (order != null) {
                userToOrder.remove(order.getUserId());
                activeOrders.remove(orderId);
            }
            orderLocks.remove(orderId);
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
                activeOrders.put(order.getOrderId(), new ActiveOrderItem(order)); // upsert
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<ActiveOrderItem> findAll() {
        return List.copyOf(activeOrders.values());
    }

    @Override
    public ActiveOrderItem findByUserId(String userId) {
        for (String orderId : activeOrders.keySet()) {
            ReentrantLock lock = orderLocks.get(orderId);
            if (lock == null) {
                continue;
            }

            lock.lock();
            try {
                ActiveOrderItem order = activeOrders.get(orderId);
                if (order != null && order.getUserId().equals(userId)) {
                    return order;
                }
            } finally {
                lock.unlock();
            }
        }
        return null;
    }
    @Override
    public Optional<ActiveOrderItem> findByIdForUpdate(String orderId) {
        return findById(orderId);
    }

    @Override
    public void deleteAll() {
        activeOrders.clear();
        orderLocks.clear();
        userToOrder.clear();
    }

    @Override
    public boolean markAsProcessing(String orderId){
        ReentrantLock lock = orderLocks.get(orderId);
        if(lock == null){
            throw new IllegalArgumentException("tried processing active order that was deleted or doesnt exist");
        }
        lock.lock();
        try{
            ActiveOrderItem existing = activeOrders.get(orderId);
            if(existing == null){
                throw new IllegalArgumentException("tried processing active order that was deleted or doesnt exist");
            }
            return existing.markAsProcessing();
        }
        finally {
            lock.unlock();
        }
    }
    @Override
    public boolean markAsNotProcessing(String orderId){
        ReentrantLock lock = orderLocks.get(orderId);
        if(lock == null){
            throw new IllegalArgumentException("tried processing active order that was deleted or doesnt exist");
        }
        lock.lock();
        try{
            ActiveOrderItem existing = activeOrders.get(orderId);
            if(existing == null){
                throw new IllegalArgumentException("tried processing active order that was deleted or doesnt exist");
            }
            return existing.markAsNotProcessing();
        }
        finally {
            lock.unlock();
        }
    }
}