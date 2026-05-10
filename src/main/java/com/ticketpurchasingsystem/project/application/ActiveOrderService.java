package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

import com.ticketpurchasingsystem.project.domain.Utils.IdGenerator;

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
    public ActiveOrderService(ActiveOrderListener activeOrderListener, ActiveOrderPublisher activeOrderPublisher, IActiveOrderRepo activeOrderRepo, AuthenticationService authenticationService, IBarCodeGateway barCodeGateway) {
        this.activeOrderListener = activeOrderListener;
        this.activeOrderPublisher = activeOrderPublisher;
        this.activeOrderRepo = activeOrderRepo;
        this.authenticationService = authenticationService;
        this.barCodeGateway = barCodeGateway;
    }

    @Override
    public void cancelActiveOrder(String orderId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getActiveOrders(String userId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getActiveOrder(String orderId) {
        // TODO Auto-generated method stub

    }


    //changed signature from completeActiveOrder to createPendingOrder, since completeActiveOrder should be called after payment is successful, and createPendingOrder should be called when the user finishes choosing the tickets and wants to checkout
    //called after "checkout" is pressed in UI
    public ActiveOrderItem createPendingOrder(SessionToken sessionToken, String userId, String eventId){
        if(authenticationService.validate(sessionToken.getToken())){
            if(activeOrderRepo.findByUserId(userId) != null){
                throw new IllegalArgumentException("an active order already exists for this user: "+ userId);
            }
            String orderId = ""+ IdGenerator.getInstance().nextId();
            ActiveOrderItem orderItem = new ActiveOrderItem(orderId,userId,eventId);
            boolean saved = saveOrder(orderItem);
            if(!saved){
                throw new RuntimeException("failed to save the order");
            }
            return orderItem;
        }
        else{
            throw new RuntimeException("the session has ended");
        }
    }

    @Override
    public void addSeatsToActiveOrder(SessionToken sessionToken, String orderId, List<String> seatIds) {
        if(authenticationService.validate(sessionToken.getToken())){
            ActiveOrderItem order = activeOrderRepo.findById(orderId);
            if (order == null) {
                throw new IllegalArgumentException("Order not found");
            }
            checkIfExpiredAndThrowException(order);
            boolean reserved = activeOrderPublisher.publishReserveSeats(order.getEventId(), seatIds);
            if (!reserved) {
                throw new IllegalStateException("cant reserve these seats");
            }
            order.addSeatIds(seatIds);
            saveOrder(order);

        }      
        else{
            throw new RuntimeException("the session has ended");
        }
    }

    public void addStandingAreaToActiveOrder(SessionToken sessionToken, String orderId, String areaId, int quantity) {
        if(authenticationService.validate(sessionToken.getToken())){
            ActiveOrderItem order = activeOrderRepo.findById(orderId);
            if (order == null) {
                throw new IllegalArgumentException("Order not found");
            }
            checkIfExpiredAndThrowException(order);
            boolean reserved = activeOrderPublisher.publishReserveStandingArea(order.getEventId(), areaId, quantity);
            if (!reserved) {
                throw new IllegalStateException("cant reserve these standing area tickets");
            }
            order.addStandingAreaQuantity(areaId, quantity);
            saveOrder(order);
        }      
        else{
            throw new RuntimeException("the session has ended");
        }
    }
//
//    @Override
//    public ActiveOrderItem createPendingOrder(SessionToken sessionToken, String userId, String eventId, int quantity) {
//        if(authenticationService.validateToken(sessionToken.getToken())){
//            boolean reserved = activeOrderPublisher.publishReserveTickets(eventId, quantity);
//            if(!reserved){
//                return null;
//            }
//            String orderId = ""+ IdGenerator.getInstance().nextId();
//            ActiveOrderItem orderItem = new ActiveOrderItem(orderId,userId,eventId, quantity);
//            saveOrder(orderItem);
//            return orderItem;
//            }
//        else{
//            throw new RuntimeException("the session has ended");
//        }
//    }

    // gets paymentGateway because there multiple gateways each for a different payment method(paypal, bit...), so gets the payment method from UI after the user chose it
    public List<BarcodeDTO> completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount, String orderId){
        if(!authenticationService.validate(sessionToken.getToken())){
            throw new RuntimeException("the session has ended");
        }
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if(order == null){
            throw new IllegalArgumentException("Order not found");
        }
        boolean processing = activeOrderRepo.markAsProcessing(orderId);
        if(!processing){
            throw new IllegalStateException("order is already being processed");
        }
        ActiveOrderDTO orderDTO = new ActiveOrderDTO(order);
        checkIfExpiredAndThrowException(order);
        //check purchasePolicy
        boolean upToPolicy = activeOrderPublisher.publishIsUpToPolicy(orderDTO);
        if(!upToPolicy){
            throw new IllegalStateException("Order violates purchase policies");
        }
        boolean paymentResult = payment(paymentGateway, sessionToken, amount);
        if(!paymentResult){
            rollbackOrderReservations(orderDTO);
            activeOrderRepo.delete(orderId);
            throw new IllegalStateException("Payment failed");
        }
        List<BarcodeDTO> barcodesIssued = barCodeGateway.issueBarcodes(orderDTO);
        if(barcodesIssued == null){
            paymentGateway.refund( orderId , amount );
            rollbackOrderReservations(orderDTO);
            activeOrderRepo.delete(orderId);
            throw new IllegalStateException("Barcode generation failed. Refund processed.");
        }
        activeOrderPublisher.publishCompletedOrder(orderDTO, amount);
        activeOrderRepo.delete(orderId);
        return barcodesIssued;
    }

    private void rollbackOrderReservations(String eventID, List<String> seatsToRollback, Map<String, Integer> standingToRollback) {
        if (seatsToRollback != null && !seatsToRollback.isEmpty()) {
                activeOrderPublisher.publishReleaseSeats(eventID, seatsToRollback);
        }
        if (standingToRollback != null && !standingToRollback.isEmpty()) {
            for (Map.Entry<String, Integer> entry : standingToRollback.entrySet()) {
                    activeOrderPublisher.publishReleaseStandingArea(eventID, entry.getKey(), entry.getValue());
            }
        }
    }
    private void rollbackOrderReservations(ActiveOrderDTO order) {
        // Unreserve specific seats
        if (order.getSeatIds() != null && !order.getSeatIds().isEmpty()) {
            List<String> seatsArray = order.getSeatIds();
            activeOrderPublisher.publishReleaseSeats(order.getEventId(), seatsArray);
        }

        // Unreserve standing area quantities
        if (order.getStandingAreaQuantities() != null && !order.getStandingAreaQuantities().isEmpty()) {
            for (HashMap.Entry<String, Integer> entry : order.getStandingAreaQuantities().entrySet()) {
                activeOrderPublisher.publishReleaseStandingArea(order.getEventId(), entry.getKey(), entry.getValue());
            }
        }
    }
//    @Override
//    public void completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount, String orderId)
//    {
//        if(authenticationService.validate(sessionToken.getToken())){
//            ActiveOrderItem order = activeOrderRepo.findById(orderId);
//            if(order == null){
//                throw new IllegalArgumentException("Order not found");
//            }
//            checkIfExpiredAndThrowException(order);
//
//            //boolean paymentResult = activeOrderPublisher.publishPaymentEvent(paymentGateway, sessionToken, amount);
//            boolean paymentResult = payment(paymentGateway, sessionToken, amount);
//            if(paymentResult) {
//                //add to history order repo and delete from active order repo
//
//                activeOrderRepo.delete(orderId);
//            }
//            else
//            {
//                activeOrderPublisher.publishUnreserveTickets(order.getEventId(), order.getQuantity());
//                activeOrderRepo.delete(orderId);
//                throw new IllegalStateException("Payment failed");
//            }
//        }
//        else{
//            throw new RuntimeException("the session has ended");
//        }
//    }

    private boolean payment(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount) {
        if(authenticationService.validate(sessionToken.getToken())) {
            return paymentGateway.pay(); // Placeholder return value, replace with actual payment processing login
            //throw new UnsupportedOperationException("Unimplemented method 'payment'");
        }else{
            throw new RuntimeException("the session has ended");
        }
    }

//    public void updateActiveOrder(SessionToken sessionToken, ActiveOrderDTO orderDTO){
//        if(!authenticationService.validate(sessionToken.getToken())){
//            throw new RuntimeException("Session has ended");
//        }
//        checkIfExpiredAndThrowException(orderDTO);
//        activeOrderRepo.save(orderDTO);
//    }

    public void updateActiveOrder(SessionToken sessionToken, ActiveOrderDTO newOrderDTO){
        if(!authenticationService.validate(sessionToken.getToken())){
            throw new RuntimeException("Session has ended");
        }
        ActiveOrderItem order = activeOrderRepo.findById(newOrderDTO.getOrderId());
        if(order == null){
            throw new IllegalArgumentException("couldn't find order to edit");
        }
        checkIfExpiredAndThrowException(order);

        List<String> currentSeats = order.getSeatIds();
        List<String> newOrderSeats = newOrderDTO.getSeatIds();
        HashMap<String, Integer> currentStanding = order.getStandingAreaQuantities();
        HashMap<String, Integer> newOrderStanding = newOrderDTO.getStandingAreaQuantities();

        List<String> seatsToReserve = new ArrayList<>(newOrderSeats);
        seatsToReserve.removeAll(currentSeats);
        List<String> seatsToRelease = new ArrayList<>(currentSeats);
        seatsToRelease.removeAll(newOrderSeats);
        Map<String, Integer> standingToReserve = calculateStandingToReserve(currentStanding, newOrderStanding);

        //try to reserve the seats
        boolean seatsReserved = activeOrderPublisher.publishReserveSeats(order.getEventId(), seatsToReserve);
        if(!seatsReserved){
            rollbackOrderReservations(order.getEventId(), seatsToReserve, null);
            throw new RuntimeException("couldnt reserve the new seats, didnt release current seats and tickets");
        }

        boolean standingReserved = true;
        for (Map.Entry<String, Integer> entry : standingToReserve.entrySet()) {
            standingReserved = activeOrderPublisher.publishReserveStandingArea(
                    order.getEventId(), entry.getKey(), entry.getValue()
            );
            if (!standingReserved) {
                break;
            }
        }
        if(!standingReserved){
            rollbackOrderReservations(order.getEventId(), seatsToReserve, standingToReserve);
            throw new RuntimeException("couldnt reserve the new standing area tickes, didnt release current seats and tickets");
        }

        order.setSeatIds(newOrderSeats);
        order.setStandingAreaQuantities(newOrderStanding);
        activeOrderRepo.update(order);

        //if we managed to reserve all the seats/tickets and update DB, we release the unneeded ones:
        activeOrderPublisher.publishReleaseSeats(order.getEventId(), seatsToRelease);
        for(String areaId : newOrderStanding.keySet()) {
            int currentQuantity = currentStanding.getOrDefault(areaId, 0); // Get current quantity for this area, default to 0 if not present
            int newQuantity = newOrderStanding.get(areaId);
            if (newQuantity < currentQuantity) {
                activeOrderPublisher.publishReleaseStandingArea(order.getEventId(), areaId, currentQuantity - newQuantity);
            }
        }
        //for each old standing area quantity that is not in the new order, release all the quantity
        for (String areaId : currentStanding.keySet()) {
            if (!newOrderStanding.containsKey(areaId)) {
                activeOrderPublisher.publishReleaseStandingArea(order.getEventId(), areaId, currentStanding.get(areaId));
            }
        }
    }
    private Map<String, Integer> calculateStandingToReserve(Map<String, Integer> currentStanding,
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
    // @Override
    // public void updateActiveOrder(SessionToken sessionToken, String orderId, int newQuantity) 
    // {
    //     if(authenticationService.validate(sessionToken.getToken())) {
    //         ActiveOrderItem order = activeOrderRepo.findById(orderId);
    //         if (order == null) {
    //             throw new IllegalArgumentException("Order not found");
    //         }
    //         checkIfExpiredAndThrowException(order);
    //         if (newQuantity <= 0) {
    //             throw new IllegalArgumentException("Quantity must be greater than 0");
    //         }

    //         if (newQuantity == order.getQuantity()) {
    //             return;
    //         }
    //         if (newQuantity < order.getQuantity()) {
    //             activeOrderPublisher.publishUnreserveTickets(order.getEventId(), order.getQuantity() - newQuantity);
    //             order.setQuantity(newQuantity);
    //             activeOrderRepo.update(order);
    //             return;
    //         } else {
    //             boolean reserved = activeOrderPublisher.publishReserveTickets(order.getEventId(), newQuantity - order.getQuantity());
    //             if (!reserved) {
    //                 throw new IllegalStateException("Not enough tickets available");
    //             }
    //             order.setQuantity(newQuantity);
    //             activeOrderRepo.update(order);
    //         }
    //     }else{
    //         throw new RuntimeException("the session is over");
    //     }
    // }
   

    
    private void checkIfExpiredAndThrowException(ActiveOrderItem order){
        if(order.getCreatedAt().getTime() + ActiveOrderItem.EXPIRATION_TIME_MINUTES*60*1000 < System.currentTimeMillis())
        {
            rollbackOrderReservations(new ActiveOrderDTO(order));
            activeOrderRepo.delete(order.getOrderId());
            throw new IllegalStateException("Order has expired");
        }
    }
    private void checkIfExpiredAndThrowException(ActiveOrderDTO order){
        if(order.getCreatedAt().getTime() + ActiveOrderItem.EXPIRATION_TIME_MINUTES*60*1000 < System.currentTimeMillis())
        {
            rollbackOrderReservations(order);
            activeOrderRepo.delete(order.getOrderId());
            throw new IllegalStateException("Order has expired");
        }
    }
    @Override
    public boolean saveOrder(ActiveOrderItem order) {
        try{
            if(order == null){
                throw new IllegalArgumentException("Order cannot be null");
            }
       
            if(!isValidEventID(order.getEventId()) || !isValidOrderID(order.getOrderId())) {
                throw new IllegalArgumentException("bad order ID or event ID");
            }
        return activeOrderRepo.save(order);
        }catch(Exception e){

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

//    public boolean startOrder(SessionToken sessionToken, String eventID) {
//        if(authenticationService.validateToken(sessionToken.getToken())){
//            try {
//                //check if user is member, if he is then need to save his order
//                boolean isMember = activeOrderPublisher.publishIsMember(sessionToken.getUserId());
//
//                // get Seating map of event and show it
//                // SeatingMapDTO map = activeOrderPublisher.publishGetSeatingMapEvent(eventID);
//                // user choose the tickets
//                /**
//                 *  need to make sure the same user cant reserve different seats/tickets from different session
//                 */
//                /**
//                 * need to start a thread that checks always if the timer has ended
//                 */
//                /**
//                 * need to publish a message to unreserve tickets if guest disconnects or 10 minutes pass for memeber
//                 */
//                String orderId = "" + IdGenerator.getInstance().nextId();
//                ActiveOrderItem orderItem = new ActiveOrderItem(orderId, sessionToken.getUserId(), eventID, 0);
//                Scanner scanner = new Scanner(System.in);
//                boolean finished = false;
//                while (!finished) {
//                    System.out.println("choose the number of tickets to buy, or choose 'finish' ");
//                    String input = scanner.nextLine();
//                    if (input.toLowerCase().equals("finish")) {
//                        break;
//                    }
//                    int inputInt;
//                    try {
//                        inputInt = Integer.parseInt(input);
//                        if (inputInt <= 0) {
//                            System.out.println("please enter a positive number of tickets");
//                        }
//                    } catch (NumberFormatException e) {
//                        System.out.println("please enter either 'finish' or a number of tickets to buy");
//                        continue;
//                    }
//                    boolean reservedTickets = activeOrderPublisher.publishReserveTickets(eventID, inputInt);
//                    if (!reservedTickets) {
//                        System.out.println("there are not enough tickets left currently ");
//                    } else {
//                        orderItem.setQuantity(inputInt);
//                        if (isMember) {
//                            saveOrder(orderItem);
//                        }
//                    }
//                }
//                System.out.println("you have " + orderItem.getQuantity() + " tickets in your order, would you like to checkout? (y/n)");
//                String choice = scanner.nextLine();
//                if (choice.toLowerCase().equals("y") || choice.toLowerCase().equals("yes")) {
//                    completeActiveOrder(orderId);
//                } else {
//                    System.out.println("order canceled");
//                    activeOrderPublisher.publishUnreserveTickets(eventID, orderItem.getQuantity());
//                }
//                return true;
//            }catch (Exception e){
//                System.out.println("got an error while trying to start order: "+ e.getMessage());
//                return false;
//            }
//        }
//        else{
//            System.out.println("the session has ended");
//            return false;
//        }
//    }
}
