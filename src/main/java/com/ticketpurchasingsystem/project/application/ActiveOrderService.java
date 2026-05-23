package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

import com.ticketpurchasingsystem.project.domain.Utils.IdGenerator;
import com.ticketpurchasingsystem.project.infrastructure.logging.logLevel;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;
import com.ticketpurchasingsystem.project.infrastructure.logging.logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveOrderService implements IActiveOrderService {
    ActiveOrderListener activeOrderListener;
    ActiveOrderPublisher activeOrderPublisher;
    IActiveOrderRepo activeOrderRepo;
    AuthenticationService authenticationService;
    IBarCodeGateway barCodeGateway;

    loggerDef logger = loggerDef.getInstance();

    ActiveOrderHandler activeOrderHandler;
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
    }

    @Override
    public void cancelActiveOrder(SessionToken sessionToken, String userId, String orderId) {
        logger.info("Attempting to cancel active order. userId: " + userId + ", orderId: " + orderId);

        if (!authenticationService.validate(sessionToken.getToken())) {
            logger.error("Session validation failed for userId: " + userId);
            throw new RuntimeException("the session has ended");
        }

        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if (order == null) {
            logger.error("Cancel order failed: Order not found with id: " + orderId);
            throw new IllegalArgumentException("Order not found");
        }

        boolean canCancel = activeOrderHandler.isUsersOrder(userId, order);
        // make sure the user canceling the order is the one who owns it
        if (!canCancel) {
            logger.error("Cancel order failed: User " + userId + " attempted to cancel order " + orderId + " belonging to " + order.getUserId());
            throw new IllegalArgumentException("this order does not belong to this user");
        }

        boolean processing = activeOrderRepo.markAsProcessing(orderId);
        if (!processing) {
            logger.warn("Cancel order failed: Order " + orderId + " is already being processed");
            throw new IllegalStateException("order is already being processed, cannot cancel");
        }

        rollbackOrderReservations(sessionToken.getToken(),new ActiveOrderDTO(order));
        activeOrderRepo.delete(orderId);
        logger.info("Successfully cancelled active order: " + orderId + " for user: " + userId);
    }

    @Override
    public ActiveOrderDTO getActiveOrderInfo(SessionToken sessionToken ,String orderId) throws Exception {
        logger.info("Attempting to retrieve active order info. orderId: " + orderId);
        if(!authenticationService.validate(sessionToken.getToken())){
            logger.error("Session validation failed while retrieving order info for orderId: " + orderId);
            throw new IllegalArgumentException("Session has ended");
        }
        String userId = authenticationService.getUser(sessionToken.getToken());
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if(order == null){
            logger.error("Active order not found with id: "+ orderId);
            throw new IllegalArgumentException("the active order was not found");
        }
        ActiveOrderDTO orderDTO = activeOrderHandler.getActiveOrderInfo(userId, order);
        if(orderDTO == null){
            logger.error("User " + userId + " tried seeing a different user's order: " + orderId);
            throw new IllegalArgumentException("you can only view your own active order");
        }
        logger.info("Successfully retrieved active order info for orderId: " + orderId);
        return orderDTO;
    }

    public ActiveOrderItem createPendingOrder(SessionToken sessionToken, String userId, String eventId){
        logger.info("Attempting to create pending order for userId: " + userId + ", eventId: " + eventId);
        if(authenticationService.validate(sessionToken.getToken())){
            if(activeOrderRepo.findByUserId(userId) != null){
                logger.warn("Create pending order failed: An active order already exists for user: " + userId);
                throw new IllegalArgumentException("an active order already exists for this user: "+ userId);
            }
            if(!isValidEventID(eventId)){
                logger.error("Create active order failed: event id "+ eventId + " isn't associated with en existing event");
                throw new RuntimeException(eventId + " isnt associated with any existing event");
            }
            String orderId = ""+ IdGenerator.getInstance().nextId();
            ActiveOrderItem orderItem = new ActiveOrderItem(orderId,userId,eventId);
            boolean canSave = activeOrderHandler.canCreateActiveOrder(orderItem);
            if(!canSave){
                logger.error("Create pending order failed: Failed to save the order for user: " + userId);
                throw new RuntimeException("failed to save the order");
            }
            logger.info("Saving order: " + orderItem.getOrderId());
            activeOrderRepo.save(orderItem);
//            boolean saved = saveOrder(orderItem);
//            if(!saved){
//                logger.error("Create pending order failed: Failed to save the order for user: " + userId);
//                throw new RuntimeException("failed to save the order");
//            }
            logger.info("Successfully created pending order: " + orderId + " for user: " + userId);
            return orderItem;
        }
        else{
            logger.error("Session validation failed while creating pending order for userId: " + userId);
            throw new RuntimeException("the session has ended");
        }
    }

    @Override
    public void addSeatsToActiveOrder(SessionToken sessionToken, String orderId, List<String> seatIds) {
        logger.info("Attempting to add seats to order: " + orderId);
        if(!authenticationService.validate(sessionToken.getToken())) {
            logger.error("Session validation failed while adding seats to order: " + orderId);
            throw new RuntimeException("the session has ended");
        }
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if (order == null) {
            logger.error("Add seats failed: Order not found with id: " + orderId);
            throw new IllegalArgumentException("Order not found");
        }
        checkIfExpiredAndThrowException(sessionToken.getToken(), order);
        List<String> seatsToReserve = activeOrderHandler.getSeatsToReserve(order.getSeatIds(), seatIds);
        boolean reserved = activeOrderPublisher.publishReserveSeats(sessionToken.getToken(), order.getOrderId(), order.getEventId(), seatsToReserve);
        if (!reserved) {
            logger.error("Add seats failed: Could not reserve seats for event: " + order.getEventId());
            throw new IllegalStateException("cant reserve these seats");
        }
        ActiveOrderItem newOrder = activeOrderHandler.addSeatsToActiveOrder(order, seatsToReserve);
        if(newOrder == null){
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, null, order.getOrderId());
            logger.error("failed to add seats to order : " + orderId);
            throw new RuntimeException("failed to add seats");
        }
        try {
            activeOrderRepo.update(newOrder);
//        order.addSeatIds(seatIds);
//        saveOrder(order);
            logger.info("Successfully added seats to order: " + orderId);
        }catch (Exception e){
            rollbackOrderReservations(sessionToken.getToken(),order.getEventId(), seatsToReserve, null, order.getOrderId());
            logger.error("got DB error when trying to update order: " + orderId +
                    " with seats");
        }
    }

    public void addStandingAreaToActiveOrder(SessionToken sessionToken, String orderId, String areaId, int quantity) {
        logger.info("Attempting to add standing area to order: " + orderId + ", areaId: " + areaId + ", quantity: " + quantity);
        if(!authenticationService.validate(sessionToken.getToken())) {
            logger.error("Session validation failed while adding standing area to order: " + orderId);
            throw new RuntimeException("the session has ended");
        }
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if (order == null) {
            logger.error("Add standing area failed: Order not found with id: " + orderId);
            throw new IllegalArgumentException("Order not found");
        }
        checkIfExpiredAndThrowException(sessionToken.getToken(), order);
        boolean reserved = activeOrderPublisher.publishReserveStandingArea(sessionToken.getToken(), order.getEventId(), areaId, quantity);
        if (!reserved) {
            logger.error("Add standing area failed: Could not reserve area " + areaId + " for event: " + order.getEventId());
            throw new IllegalStateException("cant reserve these standing area tickets");
        }
        ActiveOrderItem newOrder = activeOrderHandler.addStandingAreaToActiveOrder(order, areaId, quantity);
        if(newOrder == null){
            activeOrderPublisher.publishReleaseStandingArea(sessionToken.getToken(), order.getEventId(), areaId, quantity);
            logger.error("failed to add standing area tickets to order : " + orderId);
            throw new RuntimeException("failed to add standing area tickets");
        }
        try {
            activeOrderRepo.update(newOrder);
//        order.addStandingAreaQuantity(areaId, quantity);
//        saveOrder(order);
            logger.info("Successfully added standing area to order: " + orderId);
        }catch (Exception e){
            activeOrderPublisher.publishReleaseStandingArea(sessionToken.getToken(), order.getEventId(), areaId, quantity);
            logger.error("got DB error when trying to update order: " + orderId +
                    " with "+ quantity +" tickets for area "+ areaId + " of event "+ order.getEventId());
        }
    }


    public List<BarcodeDTO> completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount, String orderId){
        logger.info("Attempting to complete order: " + orderId + " with amount: " + amount);
        if(!authenticationService.validate(sessionToken.getToken())){
            logger.error("Session validation failed while completing order: " + orderId);
            throw new RuntimeException("the session has ended");
        }
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if(order == null){
            logger.error("Complete order failed: Order not found with id: " + orderId);
            throw new IllegalArgumentException("Order not found");
        }

        if (!activeOrderHandler.isUsersOrder(authenticationService.getUser(sessionToken.getToken()), order)) {
            logger.error("Complete order failed: user " + authenticationService.getUser(sessionToken.getToken()) + " attempted to complete order " + orderId + " belonging to " + order.getUserId());
            throw new IllegalArgumentException("Unauthorized: Order does not belong to the current user");
        }

        checkIfExpiredAndThrowException(sessionToken.getToken(), order);
        //check that the seats reserved for this order still reserved for this order (in case they were released by the user in another session, or by the system due to inactivity):
        List<String> seatsNotReserved = activeOrderPublisher.publishCheckSeatsReserved(sessionToken.getToken(), order.getOrderId(), order.getEventId(), order.getSeatIds());
        if(!(seatsNotReserved == null || seatsNotReserved.isEmpty() )){
            logger.error("Complete order failed: One or more seats for order " + orderId + " are no longer reserved");
            ActiveOrderItem updatedOrder = activeOrderHandler.removeSeatsFromActiveOrder(order, seatsNotReserved);
            activeOrderRepo.update(updatedOrder);
            throw new IllegalStateException("One or more seats are no longer reserved");
            
        }
        ActiveOrderDTO orderDTO = new ActiveOrderDTO(order);
        Integer companyId = activeOrderPublisher.publishGetCompanyId(order.getEventId());
        if(companyId == null){
            logger.error("Complete order failed: Could not retrieve company ID for event: " + order.getEventId());
            throw new RuntimeException("couldn't retrieve company information for this event");
        }

        //check purchasePolicy
        int age = getUserAge(sessionToken);
        boolean upToPolicy = activeOrderPublisher.publishIsUpToPolicy(orderDTO, age);
        if(!upToPolicy){
            logger.error("Complete order failed: Order " + orderId + " violates purchase policies");
            throw new IllegalStateException("Order violates purchase policies");
        }
        boolean processing = activeOrderRepo.markAsProcessing(orderId);
        if(!processing){
            logger.warn("Complete order failed: Order " + orderId + " is already being processed");
            throw new IllegalStateException("order is already being processed");
        }
        List<BarcodeDTO> barcodesIssued = barCodeGateway.issueBarcodes(orderDTO);
        if(barcodesIssued == null){
            logger.error("Barcode generation failed for order: " + orderId + ". Rolling back and deleting order.");
            rollbackOrderReservations(sessionToken.getToken(), orderDTO);
            activeOrderRepo.delete(orderId);
            throw new IllegalStateException("Barcode generation failed. Refund processed.");
        }

        boolean paymentResult = payment(paymentGateway, sessionToken, amount);
        if(!paymentResult){
            logger.error("Payment failed for order: " + orderId + ". Rolling back and deleting order.");
            rollbackOrderReservations(sessionToken.getToken(), orderDTO);
            activeOrderRepo.delete(orderId);
            throw new IllegalStateException("Payment failed");
        }
        activeOrderPublisher.publishCompletedOrder(orderDTO, amount, companyId);
        activeOrderRepo.delete(orderId);
        logger.info("Successfully completed order: " + orderId);
        return barcodesIssued;
    }

    private void rollbackOrderReservations(String sessionToken, String eventID, List<String> seatsToRollback, Map<String, Integer> standingToRollback, String orderId) {
        logger.info("Rolling back reservations for eventID: " + eventID);
        boolean canReleaseSeats = activeOrderHandler.canReleaseSeats(seatsToRollback);
        boolean canReleaseStanding = activeOrderHandler.canReleaseStanding(standingToRollback);
        if (canReleaseSeats) {
            activeOrderPublisher.publishReleaseSeats(sessionToken, orderId, eventID, seatsToRollback);
        }
        if (canReleaseStanding) {
            for (Map.Entry<String, Integer> entry : standingToRollback.entrySet()) {
                activeOrderPublisher.publishReleaseStandingArea(sessionToken, eventID, entry.getKey(), entry.getValue());
            }
        }
    }

    private void rollbackOrderReservations(String sessionToken, ActiveOrderDTO order) {
        logger.info("Rolling back reservations for order: " + order.getOrderId());
        boolean canReleaseSeats = activeOrderHandler.canReleaseSeats(order.getSeatIds());
        boolean canReleaseStanding = activeOrderHandler.canReleaseStanding(order.getStandingAreaQuantities());
        // Unreserve specific seats
        if (canReleaseSeats) {
            List<String> seatsArray = order.getSeatIds();
            activeOrderPublisher.publishReleaseSeats(sessionToken, order.getOrderId(), order.getEventId(), seatsArray);
        }
        // Unreserve standing area quantities
        if (canReleaseStanding) {
            for (HashMap.Entry<String, Integer> entry : order.getStandingAreaQuantities().entrySet()) {
                activeOrderPublisher.publishReleaseStandingArea(sessionToken, order.getEventId(), entry.getKey(), entry.getValue());
            }
        }
    }

    private boolean payment(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount) {
        if(authenticationService.validate(sessionToken.getToken())) {
            return paymentGateway.pay(); // Placeholder return value, replace with actual payment processing login
        }else{
            logger.error("Session validation failed during payment processing");
            throw new RuntimeException("the session has ended");
        }
    }

    public void updateActiveOrder(SessionToken sessionToken, ActiveOrderDTO newOrderDTO){
        logger.info("Attempting to update active order: " + newOrderDTO.getOrderId());
        if(!authenticationService.validate(sessionToken.getToken())){
            logger.error("Session validation failed while updating order: " + newOrderDTO.getOrderId());
            throw new RuntimeException("Session has ended");
        }
        ActiveOrderItem order = activeOrderRepo.findById(newOrderDTO.getOrderId());
        if(order == null){
            logger.error("Update order failed: Order not found with id: " + newOrderDTO.getOrderId());
            throw new IllegalArgumentException("couldn't find order to edit");
        }
        checkIfExpiredAndThrowException(sessionToken.getToken(), order);

        List<String> currentSeats = order.getSeatIds();
        List<String> newOrderSeats = newOrderDTO.getSeatIds();
        HashMap<String, Integer> currentStanding = order.getStandingAreaQuantities();
        HashMap<String, Integer> newOrderStanding = newOrderDTO.getStandingAreaQuantities();

//        List<String> seatsToReserve = new ArrayList<>(newOrderSeats);
//        seatsToReserve.removeAll(currentSeats);
        List<String> seatsToReserve = activeOrderHandler.getSeatsToReserve(currentSeats, newOrderSeats);
//        List<String> seatsToRelease = new ArrayList<>(currentSeats);
//        seatsToRelease.removeAll(newOrderSeats);
        List<String> seatsToRelease = activeOrderHandler.getSeatsToRelease(currentSeats, newOrderSeats);
        Map<String, Integer> standingToReserve = activeOrderHandler.calculateStandingToReserve(currentStanding, newOrderStanding);

        //try to reserve the seats
        boolean seatsReserved = activeOrderPublisher.publishReserveSeats(sessionToken.getToken(), order.getOrderId(), order.getEventId(), seatsToReserve);
        if(!seatsReserved){
            logger.error("Update order failed: Could not reserve new seats for order: " + order.getOrderId());
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, null, order.getOrderId());
            throw new RuntimeException("couldnt reserve the new seats, didnt release current seats and tickets");
        }

        boolean standingReserved = true;
        Map<String, Integer> successfulReserves = new HashMap<>();
        for (Map.Entry<String, Integer> entry : standingToReserve.entrySet()) {
            standingReserved = activeOrderPublisher.publishReserveStandingArea(
                    sessionToken.getToken(), order.getEventId(), entry.getKey(), entry.getValue()
            );
            if (!standingReserved) {
                break;
            }
            else{
                successfulReserves.put(entry.getKey(), entry.getValue());
            }
        }
        if(!standingReserved){
            logger.error("Update order failed: Could not reserve new standing area tickets for order: " + order.getOrderId());
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, successfulReserves, order.getOrderId());
            throw new RuntimeException("couldnt reserve the new standing area tickes, didnt release current seats and tickets");
        }
        ActiveOrderItem updatedOrder = activeOrderHandler.setNewTickets(order, newOrderSeats,newOrderStanding );
//        order.setSeatIds(newOrderSeats);
//        order.setStandingAreaQuantities(newOrderStanding);
//        activeOrderRepo.update(order);
        try {
            activeOrderRepo.update(updatedOrder);

            //if we managed to reserve all the seats/tickets and update DB, we release the unneeded ones:
            activeOrderPublisher.publishReleaseSeats(sessionToken.getToken(), order.getOrderId(), order.getEventId(), seatsToRelease);
        }catch (Exception e){
            rollbackOrderReservations(sessionToken.getToken(), order.getEventId(), seatsToReserve, standingToReserve, order.getOrderId());
        }
            // Calculate the exact standing area quantities that need to be released
            Map<String, Integer> standingAreaTicketsToRelease = activeOrderHandler.calculateStandingToRelease(currentStanding, newOrderStanding);

            // Publish a release event for each area identified by the handler
            for (Map.Entry<String, Integer> entry : standingAreaTicketsToRelease.entrySet()) {
                String areaId = entry.getKey();
                int quantityToRelease = entry.getValue();
                activeOrderPublisher.publishReleaseStandingArea(sessionToken.getToken(), order.getEventId(), areaId, quantityToRelease);
            }
            logger.info("Successfully updated order: " + order.getOrderId());
    }

//    private Map<String, Integer> calculateStandingToReserve(Map<String, Integer> currentStanding,
//                                                            Map<String, Integer> newOrderStanding) {
//        Map<String, Integer> standingToReserve = new HashMap<>();
//        for (Map.Entry<String, Integer> entry : newOrderStanding.entrySet()) {
//            String areaId = entry.getKey();
//            int newQuantity = entry.getValue();
//            int currentQuantity = currentStanding.getOrDefault(areaId, 0);
//            int difference = newQuantity - currentQuantity;
//
//            // Only keep the areas where we need to reserve additional tickets
//            if (difference > 0) {
//                standingToReserve.put(areaId, difference);
//            }
//        }
//        return standingToReserve;
//    }

    private void checkIfExpiredAndThrowException(String sessionToken, ActiveOrderItem order){
        if(activeOrderHandler.isOrderExpired(order))
        {
            logger.warn("Order " + order.getOrderId() + " has expired. Deleting and rolling back.");
            rollbackOrderReservations(sessionToken, new ActiveOrderDTO(order));
            activeOrderRepo.delete(order.getOrderId());
            throw new IllegalStateException("Order has expired");
        }
    }

    private void checkIfExpiredAndThrowException(String sessionToken, ActiveOrderDTO order){
        if(activeOrderHandler.isOrderExpired(order))
        {
            logger.warn("Order DTO " + order.getOrderId() + " has expired. Deleting and rolling back.");
            rollbackOrderReservations(sessionToken, order);
            activeOrderRepo.delete(order.getOrderId());
            throw new IllegalStateException("Order has expired");
        }
    }

    //no need for this function
    @Override
    public boolean saveOrder(ActiveOrderItem order) {
        try{
            if(order == null){
                logger.error("Save order failed: Order is null");
                throw new IllegalArgumentException("Order cannot be null");
            }

            if(!isValidEventID(order.getEventId()) || !isValidOrderID(order.getOrderId())) {
                logger.error("Save order failed: Invalid order ID or event ID for order: " + order.getOrderId());
                throw new IllegalArgumentException("bad order ID or event ID");
            }
            logger.info("Saving order: " + order.getOrderId());
            activeOrderRepo.save(order);
            return true;
        }catch(Exception e){
            logger.error("Exception occurred while saving order: " + (order != null ? order.getOrderId() : "null") + ". Error: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidEventID(String eventId) {
        return activeOrderPublisher.publishIsValidEventIDEvent(eventId);
    }

    private boolean isValidOrderID(String orderId) {
        //TODO: implement this or delete this
        // Implement your logic to validate the order ID here
        return true; // Placeholder return value
    }
    private int getUserAge(SessionToken sessionToken) {
        //TODO: implement this
        return 1;

    }
}