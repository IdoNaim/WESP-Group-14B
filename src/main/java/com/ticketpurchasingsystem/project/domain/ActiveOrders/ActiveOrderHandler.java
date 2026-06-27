package com.ticketpurchasingsystem.project.domain.ActiveOrders;

import org.springframework.stereotype.Component;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;
import java.util.*;

@Component
public class ActiveOrderHandler {
    private final loggerDef logger = loggerDef.getInstance();

    // ==========================================
    // Validation & Ownership Logic
    // ==========================================

    public ActiveOrderDTO getActiveOrderInfo(String userId, ActiveOrderItem order) {
        if (userId == null || order == null || order.getUserId() == null || order.getOrderId() == null || !userId.equals(order.getUserId())) {
            return null;
        }
        return new ActiveOrderDTO(order);
    }

    public boolean isUsersOrder(String userId, ActiveOrderItem order) {
        if (order == null || userId == null) return false;
        return order.getUserId().equals(userId);
    }

    public void validateOrderOwnership(String userId, ActiveOrderItem order, String errorMessage) {
        if (!isUsersOrder(userId, order)) {
            logger.error("Ownership validation failed: User " + userId + " does not own order " + (order != null ? order.getOrderId() : "null"));
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public void validateOrderOwnershipWithSecurityException(String userId, ActiveOrderItem order) {
        if (!isUsersOrder(userId, order)) {
            logger.error("Security violation: User " + userId + " does not own order " + (order != null ? order.getOrderId() : "null"));
            throw new SecurityException("You don't own this order");
        }
    }

    public void validatePendingOrderCreation(ActiveOrderItem existingOrder, boolean isValidEvent, String eventId, String userId) {
        if (existingOrder != null) {
            logger.warn("Create pending order failed: An active order already exists for user: " + userId);
            throw new IllegalArgumentException("an active order already exists for this user: " + userId);
        }
        if (!isValidEvent) {
            logger.error("Create active order failed: event id " + eventId + " isn't associated with an existing event");
            throw new RuntimeException(eventId + " isnt associated with any existing event");
        }
    }

    public boolean canCreateActiveOrder(ActiveOrderItem order) {
        if (order == null) {
            logger.error("Save order failed: Order is null");
            throw new IllegalArgumentException("Order cannot be null");
        }
//        if (!isValidOrderID(order.getOrderId())) {
//            logger.error("Save order failed: Invalid order ID for order: " + order.getOrderId());
//            throw new IllegalArgumentException("bad order ID");
//        }
        return true;
    }

    public boolean isValidOrderID(String orderId) {
        return orderId != null && Long.parseLong(orderId) >= 0;
    }

    // ==========================================
    // Expiration Logic
    // ==========================================

    public boolean isOrderExpired(ActiveOrderItem order) {
        return order.getCreatedAt().getTime() + ActiveOrderItem.EXPIRATION_TIME_MINUTES * 60 * 1000 < System.currentTimeMillis();
    }

    public boolean isOrderExpired(ActiveOrderDTO order) {
        return order.getCreatedAt().getTime() + ActiveOrderItem.EXPIRATION_TIME_MINUTES * 60 * 1000 < System.currentTimeMillis();
    }

    public boolean canReleaseSeats(List<String> seatsToRelease) {
        return seatsToRelease != null && !seatsToRelease.isEmpty();
    }

    public boolean canReleaseStanding(Map<String, Integer> standingToRelease) {
        return standingToRelease != null && !standingToRelease.isEmpty();
    }

    // ==========================================
    // Ticket & Delta Mathematics
    // ==========================================

    public List<String> getSeatsToReserve(List<String> currentSeats, List<String> newSeats) {
        List<String> seatsToReserve = new ArrayList<>(newSeats);
        if (currentSeats != null) {
            seatsToReserve.removeAll(currentSeats);
        }
        return seatsToReserve;
    }

    public List<String> getSeatsToRelease(List<String> currentSeats, List<String> newSeats) {
        List<String> seatsToRelease = new ArrayList<>(currentSeats);
        if (newSeats != null) {
            seatsToRelease.removeAll(newSeats);
        }
        return seatsToRelease;
    }

    public Map<String, Integer> calculateStandingToReserve(Map<String, Integer> currentStanding, Map<String, Integer> newOrderStanding) {
        Map<String, Integer> standingToReserve = new HashMap<>();
        if (newOrderStanding == null) return standingToReserve;

        for (Map.Entry<String, Integer> entry : newOrderStanding.entrySet()) {
            String areaId = entry.getKey();
            int newQuantity = entry.getValue();
            int currentQuantity = currentStanding != null ? currentStanding.getOrDefault(areaId, 0) : 0;
            int difference = newQuantity - currentQuantity;

            if (difference > 0) {
                standingToReserve.put(areaId, difference);
            }
        }
        return standingToReserve;
    }

    public Map<String, Integer> calculateStandingToRelease(Map<String, Integer> currentStanding, Map<String, Integer> newOrderStanding) {
        if (currentStanding == null) return Collections.emptyMap();
        if (newOrderStanding == null) return Collections.emptyMap();

        Map<String, Integer> standingToRelease = new HashMap<>();

        for (String areaId : currentStanding.keySet()) {
            Integer currentQtyObj = currentStanding.get(areaId);
            int currentQuantity = (currentQtyObj != null) ? currentQtyObj : 0;

            Integer newQtyObj = newOrderStanding.get(areaId);
            int newQuantity = (newQtyObj != null) ? newQtyObj : 0;

            if (newQuantity < currentQuantity) {
                standingToRelease.put(areaId, currentQuantity - newQuantity);
            }
        }
        return standingToRelease;
    }

    // ==========================================
    // State Mutation & Transformations
    // ==========================================

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

    public ActiveOrderItem setNewTickets(ActiveOrderItem order, List<String> newOrderSeats, HashMap<String, Integer> newOrderStanding) {
        if (order == null) return null;
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