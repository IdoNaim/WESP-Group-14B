package com.ticketpurchasingsystem.project.infrastructure;
import org.springframework.stereotype.Repository;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Repository
public class ActiveOrderMemRepo implements IActiveOrderRepo {

    private final ConcurrentHashMap<String, ActiveOrderItem> activeOrders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> orderLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userToOrder = new ConcurrentHashMap<>();
    private ReentrantLock getLockFor(String orderId) {
        return orderLocks.computeIfAbsent(orderId, id -> new ReentrantLock());
    }

    @Override
    public void save(ActiveOrderItem order) {
        if(order == null){
            throw new IllegalArgumentException("tried to save null active order");
        }
        String existing = userToOrder.putIfAbsent(order.getUserId(), order.getOrderId());
        if (existing != null) {
            throw new IllegalArgumentException("the user "+ order.getUserId()+ " already has an active order");
        }
        activeOrders.put(order.getOrderId(), order);
        getLockFor(order.getOrderId());
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
}

