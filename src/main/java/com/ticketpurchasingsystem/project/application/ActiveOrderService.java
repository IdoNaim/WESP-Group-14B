package com.ticketpurchasingsystem.project.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderHandler;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderListener;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderPublisher;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.IActiveOrderRepo;
import com.ticketpurchasingsystem.project.domain.Utils.IdGenerator;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Service
@Transactional
public class ActiveOrderService implements IActiveOrderService {
    private final ActiveOrderListener activeOrderListener;
    private final ActiveOrderPublisher activeOrderPublisher;
    private final IActiveOrderRepo activeOrderRepo;
    private final AuthenticationService authenticationService;
    private final IBarCodeGateway barCodeGateway;
    private final ActiveOrderHandler activeOrderHandler;

    private final loggerDef logger = loggerDef.getInstance();

    public ActiveOrderService(ActiveOrderListener activeOrderListener,
            ActiveOrderPublisher activeOrderPublisher,
            IActiveOrderRepo activeOrderRepo,
            ActiveOrderHandler activeOrderHandler,
            AuthenticationService authenticationService,
            IBarCodeGateway barCodeGateway) {

        this.activeOrderListener = activeOrderListener;
        this.activeOrderPublisher = activeOrderPublisher;
        this.activeOrderRepo = activeOrderRepo;
        this.activeOrderHandler = activeOrderHandler;
        this.authenticationService = authenticationService;
        this.barCodeGateway = barCodeGateway;

        if (activeOrderRepo == null || activeOrderListener == null || activeOrderPublisher == null
                || activeOrderHandler == null || authenticationService == null || barCodeGateway == null) {
            logger.error("ActiveOrderService initialization failed: One or more dependencies are null");
        }
    }

    @Override
    public void cancelActiveOrder(SessionToken sessionToken, String userId, String orderId) {
        logger.info("Attempting to cancel active order. userId: " + userId + ", orderId: " + orderId);
        validateSession(sessionToken, "Session validation failed for userId: " + userId, "the session has ended");
        validateUserMatch(sessionToken, userId);

        ActiveOrderItem order = activeOrderRepo.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            logger.error("Cancel order failed: Order not found with id: " + orderId);
            throw new IllegalArgumentException("Order not found");
        }

        activeOrderHandler.validateOrderOwnership(userId, order, "this order does not belong to this user");

        boolean processing = activeOrderRepo.markAsProcessing(orderId);
        if (!processing) {
            logger.warn("Cancel order failed: Order " + orderId + " is already being processed");
            throw new IllegalStateException("order is already being processed, cannot cancel");
        }
        try {
            rollbackOrderReservations(sessionToken.getToken(), new ActiveOrderDTO(order));
            activeOrderRepo.delete(orderId);
            activeOrderPublisher.publishOrderCancelled(userId, orderId);
            logger.info("Successfully cancelled active order: " + orderId + " for user: " + userId);
        }catch (Exception e){
            activeOrderRepo.markAsNotProcessing(orderId);
            logger.error("got error when trying to cancel order: "+ e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrderDTO getActiveOrderByUserId(SessionToken sessionToken, String userId) throws Exception {
        logger.info("Attempting to retrieve active order for userId: " + userId);
        validateSession(sessionToken, "Session validation failed while retrieving active order for userId: " + userId,
                "Session has ended");

        ActiveOrderItem order = activeOrderRepo.findByUserId(userId);
        if (order == null) {
            logger.warn("No active order found for userId: " + userId);
            return null;
        }

        ActiveOrderDTO orderDTO = activeOrderHandler.getActiveOrderInfo(userId, order);
        if (orderDTO == null) {
            logger.error("User " + userId + " tried seeing a different user's order: " + order.getOrderId());
            throw new IllegalArgumentException("you can only view your own active order");
        }
        logger.info("Successfully retrieved active order for userId: " + userId);
        return orderDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrderDTO getActiveOrderInfo(SessionToken sessionToken, String orderId) throws Exception {
        logger.info("Attempting to retrieve active order info. orderId: " + orderId);
        validateSession(sessionToken, "Session validation failed while retrieving order info for orderId: " + orderId,
                "Session has ended");

        String userId = authenticationService.getUser(sessionToken.getToken());
        ActiveOrderItem order = activeOrderRepo.findById(orderId).orElse(null);
        if (order == null) {
            logger.error("Active order not found with id: " + orderId);
            throw new IllegalArgumentException("the active order was not found");
        }

        ActiveOrderDTO orderDTO = activeOrderHandler.getActiveOrderInfo(userId, order);
        if (orderDTO == null) {
            logger.error("User " + userId + " tried seeing a different user's order: " + orderId);
            throw new SecurityException("you can only view your own active order");
        }
        logger.info("Successfully retrieved active order info for orderId: " + orderId);
        return orderDTO;
    }

    @Override
    public ActiveOrderItem createPendingOrder(SessionToken sessionToken, String userId, String eventId) {
        logger.info("Attempting to create pending order for userId: " + userId + ", eventId: " + eventId);
        validateSession(sessionToken, "Session validation failed while creating pending order for userId: " + userId,
                "the session has ended");
        validateUserMatch(sessionToken, userId);

        ActiveOrderItem existingOrder = activeOrderRepo.findByUserId(userId);
        boolean isValidEvent = isValidEventID(eventId);
        activeOrderHandler.validatePendingOrderCreation(existingOrder, isValidEvent, eventId, userId);

        String orderId = "" + IdGenerator.getInstance().nextId();
        ActiveOrderItem orderItem = new ActiveOrderItem(orderId, userId, eventId);

        activeOrderHandler.canCreateActiveOrder(orderItem);

        logger.info("Saving order: " + orderItem.getOrderId());
        activeOrderRepo.save(orderItem);
        logger.info("Successfully created pending order: " + orderId + " for user: " + userId);
        return orderItem;
    }

    @Override
    public void addSeatsToActiveOrder(SessionToken sessionToken, String orderId, List<String> seatIds) {
        logger.info("Attempting to add seats to order: " + orderId);
        validateSession(sessionToken, "Session validation failed while adding seats to order: " + orderId,
                "the session has ended");

        ActiveOrderItem order = activeOrderRepo.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            logger.error("Add seats failed: Order not found with id: " + orderId);
            throw new IllegalArgumentException("Order not found");
        }

        String tokenUserSeats = authenticationService.getUser(sessionToken.getToken());
        activeOrderHandler.validateOrderOwnershipWithSecurityException(tokenUserSeats, order);

        checkIfExpiredAndThrowException(sessionToken.getToken(), order);
        List<String> seatsToReserve = activeOrderHandler.getSeatsToReserve(order.getSeatIds(), seatIds);

        ActiveOrderDTO previewDTO = new ActiveOrderDTO(order);
        List<String> previewSeats = new ArrayList<>(previewDTO.getSeatIds());
        previewSeats.addAll(seatsToReserve);
        previewDTO.setSeatIds(previewSeats);
        if (!activeOrderPublisher.publishIsUpToPolicy(previewDTO, getUserAge(sessionToken))) {
            logger.error("Add seats failed: Adding seats would exceed purchase policy limit for order: " + orderId);
            throw new IllegalStateException("Cannot add tickets: purchase policy limit would be exceeded");
        }

        boolean reserved = activeOrderPublisher.publishReserveSeats(sessionToken.getToken(), order.getOrderId(),
                order.getEventId(), seatsToReserve);
        if (!reserved) {
            logger.error("Add seats failed: Could not reserve seats for event: " + order.getEventId());
            throw new IllegalStateException("cant reserve these seats");
        }

        ActiveOrderItem newOrder = activeOrderHandler.addSeatsToActiveOrder(order, seatsToReserve);
        if (newOrder == null) {
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, null,
                    order.getOrderId());
            logger.error("failed to add seats to order : " + orderId);
            throw new RuntimeException("failed to add seats");
        }
        try {
            activeOrderRepo.update(newOrder);
            logger.info("Successfully added seats to order: " + orderId);
        } catch (Exception e) {
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, null,
                    order.getOrderId());
            logger.error("got DB error when trying to update order: " + orderId + " with seats");
        }
    }

    @Override
    public void addStandingAreaToActiveOrder(SessionToken sessionToken, String orderId, String areaId, int quantity) {
        logger.info("Attempting to add standing area to order: " + orderId + ", areaId: " + areaId + ", quantity: "
                + quantity);
        validateSession(sessionToken, "Session validation failed while adding standing area to order: " + orderId,
                "the session has ended");

        ActiveOrderItem order = activeOrderRepo.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            logger.error("Add standing area failed: Order not found with id: " + orderId);
            throw new IllegalArgumentException("Order not found");
        }

        String tokenUserStanding = authenticationService.getUser(sessionToken.getToken());
        activeOrderHandler.validateOrderOwnershipWithSecurityException(tokenUserStanding, order);

        checkIfExpiredAndThrowException(sessionToken.getToken(), order);

        ActiveOrderDTO previewStandingDTO = new ActiveOrderDTO(order);
        HashMap<String, Integer> previewStanding = previewStandingDTO.getStandingAreaQuantities();
        previewStanding.merge(areaId, quantity, Integer::sum);
        previewStandingDTO.setStandingAreaQuantities(previewStanding);
        if (!activeOrderPublisher.publishIsUpToPolicy(previewStandingDTO, getUserAge(sessionToken))) {
            logger.error("Add standing area failed: Adding tickets would exceed purchase policy limit for order: "
                    + orderId);
            throw new IllegalStateException("Cannot add tickets: purchase policy limit would be exceeded");
        }

        boolean reserved = activeOrderPublisher.publishReserveStandingArea(sessionToken.getToken(), order.getEventId(),
                areaId, quantity);
        if (!reserved) {
            logger.error(
                    "Add standing area failed: Could not reserve area " + areaId + " for event: " + order.getEventId());
            throw new IllegalStateException("cant reserve these standing area tickets");
        }

        ActiveOrderItem newOrder = activeOrderHandler.addStandingAreaToActiveOrder(order, areaId, quantity);
        if (newOrder == null) {
            activeOrderPublisher.publishReleaseStandingArea(sessionToken.getToken(), order.getEventId(), areaId,
                    quantity);
            logger.error("failed to add standing area tickets to order : " + orderId);
            throw new RuntimeException("failed to add standing area tickets");
        }
        try {
            activeOrderRepo.update(newOrder);
            logger.info("Successfully added standing area to order: " + orderId);
        } catch (Exception e) {
            activeOrderPublisher.publishReleaseStandingArea(sessionToken.getToken(), order.getEventId(), areaId,
                    quantity);
            logger.error("got DB error when trying to update order: " + orderId + " with " + quantity
                    + " tickets for area " + areaId + " of event " + order.getEventId());
        }
    }

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public List<BarcodeDTO> completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken,
            PaymentDetails paymentDetails, String orderId) {
        double amount = paymentDetails.getAmount();
        logger.info("Attempting to complete order: " + orderId + " with amount: " + amount);
        validateSession(sessionToken, "Session validation failed while completing order: " + orderId,
                "the session has ended");

        ActiveOrderItem order = activeOrderRepo.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            logger.error("Complete order failed: Order not found with id: " + orderId);
            throw new IllegalArgumentException("Order not found");
        }

        String currentTokenUser = authenticationService.getUser(sessionToken.getToken());
        activeOrderHandler.validateOrderOwnership(currentTokenUser, order,
                "Unauthorized: Order does not belong to the current user");

        checkIfExpiredAndThrowException(sessionToken.getToken(), order);

        List<String> seatsNotReserved = activeOrderPublisher.publishCheckSeatsReserved(sessionToken.getToken(),
                order.getOrderId(), order.getEventId(), order.getSeatIds());
        if (!(seatsNotReserved == null || seatsNotReserved.isEmpty())) {
            logger.error("Complete order failed: One or more seats for order " + orderId + " are no longer reserved");
            ActiveOrderItem updatedOrder = activeOrderHandler.removeSeatsFromActiveOrder(order, seatsNotReserved);
            activeOrderRepo.update(updatedOrder);
            throw new IllegalStateException("One or more seats are no longer reserved");
        }

        ActiveOrderDTO orderDTO = new ActiveOrderDTO(order);
        Integer companyId = activeOrderPublisher.publishGetCompanyId(order.getEventId());
        if (companyId == null) {
            logger.error("Complete order failed: Could not retrieve company ID for event: " + order.getEventId());
            throw new RuntimeException("couldn't retrieve company information for this event");
        }

        int age = getUserAge(sessionToken);
        boolean upToPolicy = activeOrderPublisher.publishIsUpToPolicy(orderDTO, age);
        if (!upToPolicy) {
            logger.error("Complete order failed: Order " + orderId + " violates purchase policies");
            throw new IllegalStateException("Order violates purchase policies");
        }

        boolean processing = activeOrderRepo.markAsProcessing(orderId);
        if (!processing) {
            logger.warn("Complete order failed: Order " + orderId + " is already being processed");
            throw new IllegalStateException("order is already being processed");
        }

        int transactionId =-1;
        try {
            transactionId = payment(paymentGateway, sessionToken, paymentDetails);
            if (transactionId == -1) {
                logger.error("Payment failed for order: " + orderId + " got -1 transactionId.");
                activeOrderRepo.markAsNotProcessing(orderId);
                throw new IllegalStateException("Payment failed");
            }
        }catch (Exception e){
            logger.error("Payment failed for order: " + orderId + " got exception when paying");
            activeOrderRepo.markAsNotProcessing(orderId);
            throw new IllegalStateException("Payment failed");
        }

        List<BarcodeDTO> barcodesIssued = null;
        try {
            barcodesIssued = barCodeGateway.issueBarcodes(orderDTO);
            if (barcodesIssued == null) {
                throw new IllegalStateException("Barcode generation failed");
            }
            activeOrderRepo.delete(orderId);
            activeOrderPublisher.publishCompletedOrder(orderDTO, amount, companyId, transactionId);
//            activeOrderRepo.delete(orderId);
            logger.info("Successfully completed order: " + orderId);
            return barcodesIssued;
        } catch (Exception e) {
            logger.error("Exception occurred during checkout completion for order " + orderId + ": " + e.getMessage());
            try {
                paymentGateway.refund(transactionId);
            } catch (Exception refundEx) {
                logger.error(
                        "Failed to refund transaction " + transactionId + " during rollback: " + refundEx.getMessage());
            }
            if (barcodesIssued != null) {
                try {
                    barCodeGateway.cancelTickets(barcodesIssued);
                } catch (Exception cancelEx) {
                    logger.error("Failed to cancel tickets during rollback: " + cancelEx.getMessage());
                }
            }
            try {
                rollbackOrderReservations(sessionToken.getToken(), orderDTO);
            } catch (Exception rollbackEx) {
                logger.error("Failed to rollback reservations during rollback: " + rollbackEx.getMessage());
            }
            try{
                activeOrderRepo.markAsNotProcessing(orderId);
            }
            catch (Exception exception){
                logger.error("Failed to mark order "+ orderId+" as not processing: "+ exception.getMessage());
            }
//            try {
//                activeOrderRepo.delete(orderId);
//            } catch (Exception deleteEx) {
//                logger.error("Failed to delete active order during rollback: " + deleteEx.getMessage());
//            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Order completion failed", e);
        }
    }

    @Override
    public void updateActiveOrder(SessionToken sessionToken, ActiveOrderDTO newOrderDTO) {
        logger.info("Attempting to update active order: " + newOrderDTO.getOrderId());
        validateSession(sessionToken, "Session validation failed while updating order: " + newOrderDTO.getOrderId(),
                "Session has ended");

        ActiveOrderItem order = activeOrderRepo.findByIdForUpdate(newOrderDTO.getOrderId()).orElse(null);
        if (order == null) {
            logger.error("Update order failed: Order not found with id: " + newOrderDTO.getOrderId());
            throw new IllegalArgumentException("couldn't find order to edit");
        }
        checkIfExpiredAndThrowException(sessionToken.getToken(), order);

        if (!activeOrderPublisher.publishIsUpToPolicy(newOrderDTO, getUserAge(sessionToken))) {
            logger.error("Update order failed: Order violates purchase policy for order: " + newOrderDTO.getOrderId());
            throw new IllegalStateException(
                    "Cannot reserve tickets: you have already purchased the maximum allowed tickets for this event");
        }

        List<String> currentSeats = order.getSeatIds();
        List<String> newOrderSeats = newOrderDTO.getSeatIds();
        HashMap<String, Integer> currentStanding = order.getStandingAreaQuantities();
        HashMap<String, Integer> newOrderStanding = newOrderDTO.getStandingAreaQuantities();

        List<String> seatsToReserve = activeOrderHandler.getSeatsToReserve(currentSeats, newOrderSeats);
        List<String> seatsToRelease = activeOrderHandler.getSeatsToRelease(currentSeats, newOrderSeats);
        Map<String, Integer> standingToReserve = activeOrderHandler.calculateStandingToReserve(currentStanding,
                newOrderStanding);

        boolean seatsReserved = activeOrderPublisher.publishReserveSeats(sessionToken.getToken(), order.getOrderId(),
                order.getEventId(), seatsToReserve);
        if (!seatsReserved) {
            logger.error("Update order failed: Could not reserve new seats for order: " + order.getOrderId());
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, null,
                    order.getOrderId());
            throw new RuntimeException("couldnt reserve the new seats, didnt release current seats and tickets");
        }

        boolean standingReserved = true;
        Map<String, Integer> successfulReserves = new HashMap<>();
        for (Map.Entry<String, Integer> entry : standingToReserve.entrySet()) {
            standingReserved = activeOrderPublisher.publishReserveStandingArea(sessionToken.getToken(),
                    order.getEventId(), entry.getKey(), entry.getValue());
            if (!standingReserved) {
                break;
            } else {
                successfulReserves.put(entry.getKey(), entry.getValue());
            }
        }
        if (!standingReserved) {
            logger.error("Update order failed: Could not reserve new standing area tickets for order: "
                    + order.getOrderId());
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, successfulReserves,
                    order.getOrderId());
            throw new RuntimeException(
                    "couldnt reserve the new standing area tickes, didnt release current seats and tickets");
        }

        ActiveOrderItem updatedOrder = activeOrderHandler.setNewTickets(order, newOrderSeats, newOrderStanding);
        try {
            activeOrderRepo.update(updatedOrder);
            activeOrderPublisher.publishReleaseSeats(sessionToken.getToken(), order.getOrderId(), order.getEventId(),
                    seatsToRelease);
        } catch (Exception e) {
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, standingToReserve,
                    order.getOrderId());
        }

        Map<String, Integer> standingAreaTicketsToRelease = activeOrderHandler
                .calculateStandingToRelease(currentStanding, newOrderStanding);
        for (Map.Entry<String, Integer> entry : standingAreaTicketsToRelease.entrySet()) {
            activeOrderPublisher.publishReleaseStandingArea(sessionToken.getToken(), order.getEventId(), entry.getKey(),
                    entry.getValue());
        }
        logger.info("Successfully updated order: " + order.getOrderId());
    }

    @Override
    public boolean saveOrder(ActiveOrderItem order) {
        try {
            if (order == null) {
                logger.error("Save order failed: Order is null");
                throw new IllegalArgumentException("Order cannot be null");
            }
            if (!isValidEventID(order.getEventId()) || !activeOrderHandler.isValidOrderID(order.getOrderId())) {
                logger.error("Save order failed: Invalid order ID or event ID for order: " + order.getOrderId());
                throw new IllegalArgumentException("bad order ID or event ID");
            }
            logger.info("Saving order: " + order.getOrderId());
            activeOrderRepo.save(order);
            return true;
        } catch (Exception e) {
            logger.error("Exception occurred while saving order: " + (order != null ? order.getOrderId() : "null")
                    + ". Error: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // Internal Helper Assertions & Private Hooks
    // ==========================================

    private void validateSession(SessionToken token, String logMessage, String exceptionMessage) {
        if (!authenticationService.validate(token.getToken())) {
            logger.error(logMessage);
            throw new RuntimeException(exceptionMessage);
        }
    }

    private void validateUserMatch(SessionToken token, String userId) {
        String tokenUser = authenticationService.getUser(token.getToken());
        if (!tokenUser.equals(userId)) {
            logger.error("Identity discrepancy match failed: userId " + userId + " does not match token context holder "
                    + tokenUser);
            throw new IllegalArgumentException("userId does not match authenticated user");
        }
    }

    private void rollbackOrderReservations(String sessionToken, String eventID, List<String> seatsToRollback,
            Map<String, Integer> standingToRollback, String orderId) {
        logger.info("Rolling back reservations for eventID: " + eventID);
        if (activeOrderHandler.canReleaseSeats(seatsToRollback)) {
            activeOrderPublisher.publishReleaseSeats(sessionToken, orderId, eventID, seatsToRollback);
        }
        if (activeOrderHandler.canReleaseStanding(standingToRollback)) {
            for (Map.Entry<String, Integer> entry : standingToRollback.entrySet()) {
                activeOrderPublisher.publishReleaseStandingArea(sessionToken, eventID, entry.getKey(),
                        entry.getValue());
            }
        }
    }

    private void rollbackOrderReservations(String sessionToken, ActiveOrderDTO order) {
        logger.info("Rolling back reservations for order: " + order.getOrderId());
        if (activeOrderHandler.canReleaseSeats(order.getSeatIds())) {
            activeOrderPublisher.publishReleaseSeats(sessionToken, order.getOrderId(), order.getEventId(),
                    order.getSeatIds());
        }
        if (activeOrderHandler.canReleaseStanding(order.getStandingAreaQuantities())) {
            for (Map.Entry<String, Integer> entry : order.getStandingAreaQuantities().entrySet()) {
                activeOrderPublisher.publishReleaseStandingArea(sessionToken, order.getEventId(), entry.getKey(),
                        entry.getValue());
            }
        }
    }

    private int payment(IPaymentGateway paymentGateway, SessionToken sessionToken, PaymentDetails paymentDetails) {
        if (authenticationService.validate(sessionToken.getToken())) {
            return paymentGateway.pay(paymentDetails);
        } else {
            logger.error("Session validation failed during payment processing");
            throw new RuntimeException("the session has ended");
        }
    }

    private void checkIfExpiredAndThrowException(String sessionToken, ActiveOrderItem order) {
        if (activeOrderHandler.isOrderExpired(order)) {
            logger.warn("Order " + order.getOrderId() + " has expired. Deleting and rolling back.");
            rollbackOrderReservations(sessionToken, new ActiveOrderDTO(order));
            activeOrderRepo.delete(order.getOrderId());
            throw new IllegalStateException("Order has expired");
        }
    }

    private boolean isValidEventID(String eventId) {
        return activeOrderPublisher.publishIsValidEventIDEvent(eventId);
    }

    private int getUserAge(SessionToken sessionToken) {
        return 20;
    }
}