package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import com.sun.java.accessibility.util.EventID;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

import java.util.*;

@Component
public class ActiveOrderHandler {
    private loggerDef logger = loggerDef.getInstance();

    public ActiveOrderDTO getActiveOrderInfo(String userId, ActiveOrderItem order) {
        if (userId == null || order == null || order.getUserId() == null || order.getOrderId() == null || !userId.equals(order.getUserId())) {
            return null;
        }
        ActiveOrderDTO orderDTO = new ActiveOrderDTO(order);
        return orderDTO;
    }

    public boolean isUsersOrder(String userId, ActiveOrderItem order) {
        return order.getUserId().equals(userId);
    }

    public boolean canReleaseSeats(List<String> seatsToRelease) {
        return seatsToRelease != null && !seatsToRelease.isEmpty();
    }

    public boolean canReleaseStanding(Map<String, Integer> standingToRelease) {
        return standingToRelease != null && !standingToRelease.isEmpty();
    }

    public boolean canCreateActiveOrder(ActiveOrderItem order) {
        if (order == null) {
            logger.error("Save order failed: Order is null");
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (!isValidOrderID(order.getOrderId())) {
            logger.error("Save order failed: Invalid order ID for order: " + order.getOrderId());
            throw new IllegalArgumentException("bad order ID");
        }
        return true;
    }

    public boolean isValidOrderID(String orderId) {
        return orderId != null && Long.parseLong(orderId) >= 0;
    }

    public ActiveOrderItem addSeatsToActiveOrder(ActiveOrderItem order, List<String> seatIds) {
        if (order == null || seatIds == null) {
            return null;
        }
        ActiveOrderItem newOrder = new ActiveOrderItem(order);
        newOrder.addSeatIds(seatIds);
        return newOrder;
    }

    public ActiveOrderItem addStandingAreaToActiveOrder(ActiveOrderItem order, String areaId, int quantity) {
        if (order == null || areaId == null || quantity < 0) {
            return null;
        }
        if (quantity == 0) {
            return order;
        }
        ActiveOrderItem newOrder = new ActiveOrderItem(order);
        newOrder.addStandingAreaQuantity(areaId, quantity);
        return newOrder;
    }

    public boolean isOrderExpired(ActiveOrderItem order) {
        return order.getCreatedAt().getTime() + ActiveOrderItem.EXPIRATION_TIME_MINUTES * 60 * 1000 < System.currentTimeMillis();
    }
    public boolean isOrderExpired(ActiveOrderDTO order){
        return order.getCreatedAt().getTime() + ActiveOrderItem.EXPIRATION_TIME_MINUTES * 60 * 1000 < System.currentTimeMillis();
    }
    public List<String> getSeatsToReserve(List<String> currentSeats, List<String> newSeats) {
        List<String> seatsToReserve = new ArrayList<>(newSeats);
        seatsToReserve.removeAll(currentSeats);
        return seatsToReserve;
    }

    public List<String> getSeatsToRelease(List<String> currentSeats, List<String> newSeats) {
        List<String> seatsToRelease = new ArrayList<>(currentSeats);
        seatsToRelease.removeAll(newSeats);
        return seatsToRelease;
    }

    public Map<String, Integer> calculateStandingToReserve(Map<String, Integer> currentStanding,
                                                           Map<String, Integer> newOrderStanding) {
        Map<String, Integer> standingToReserve = new HashMap<>();
        for (Map.Entry<String, Integer> entry : newOrderStanding.entrySet()) {
            String areaId = entry.getKey();
            int newQuantity = entry.getValue();
            int currentQuantity = currentStanding.getOrDefault(areaId, 0);
            int difference = newQuantity - currentQuantity;

            // Only keep the areas where we need to reserve additional tickets
            if (difference > 0) {
                standingToReserve.put(areaId, difference);
            }
        }
        return standingToReserve;
    }
    public Map<String, Integer> calculateStandingToRelease(Map<String, Integer> currentStanding,
                                                           Map<String, Integer> newOrderStanding) {
        // Defensive null-checks for the input maps themselves
        if (currentStanding == null) return Collections.emptyMap();
        if (newOrderStanding == null) return Collections.emptyMap();

        Map<String, Integer> standingToRelease = new HashMap<>();

        // We only care about areas we CURRENTLY have tickets for
        for (String areaId : currentStanding.keySet()) {
            // Safely extract current quantity, handling explicit null values
            Integer currentQtyObj = currentStanding.get(areaId);
            int currentQuantity = (currentQtyObj != null) ? currentQtyObj : 0;

            // Safely extract new quantity, handling missing keys AND explicit null values
            Integer newQtyObj = newOrderStanding.get(areaId);
            int newQuantity = (newQtyObj != null) ? newQtyObj : 0;

            if (newQuantity < currentQuantity) {
                standingToRelease.put(areaId, currentQuantity - newQuantity);
            }
        }
        return standingToRelease;
    }

    public ActiveOrderItem setNewTickets(ActiveOrderItem order, List<String> newOrderSeats, HashMap<String, Integer> newOrderStanding) {
        ActiveOrderItem newOrder = new ActiveOrderItem(order);
        newOrder.setSeatIds(newOrderSeats);
        newOrder.setStandingAreaQuantities(newOrderStanding);
        return newOrder;
    }
    public ActiveOrderItem removeSeatsFromActiveOrder(ActiveOrderItem order, List<String> seatIdsToRemove) {
        if (order == null || seatIdsToRemove == null) {
            return null;
        }
        ActiveOrderItem newOrder = new ActiveOrderItem(order);
        newOrder.removeSeatIds(seatIdsToRemove);
        return newOrder;
    }
}
