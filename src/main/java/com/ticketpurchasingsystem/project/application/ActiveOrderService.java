package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsValidEventIDEvent;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

import com.ticketpurchasingsystem.project.domain.Utils.IdGenerator;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;

import javax.naming.AuthenticationException;
import java.util.HashMap;
import java.util.List;


public class ActiveOrderService implements IActiveOrderService {
    ActiveOrderListener activeOrderListener;
    ActiveOrderPublisher activeOrderPublisher;
    IActiveOrderRepo activeOrderRepo;
    AuthenticationService authenticationService;
    IBarCodeGateway barCodeGateway;
    public ActiveOrderService(ActiveOrderListener activeOrderListener, ActiveOrderPublisher activeOrderPublisher, IActiveOrderRepo activeOrderRepo, AuthenticationService authenticationService) {
        this.activeOrderListener = activeOrderListener;
        this.activeOrderPublisher = activeOrderPublisher;
        this.activeOrderRepo = activeOrderRepo;
        this.authenticationService = authenticationService;
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
    public ActiveOrderDTO createPendingOrder(SessionToken sessionToken, String userId, String eventId){
        if(authenticationService.validate(sessionToken.getToken())){
            String orderId = ""+ IdGenerator.getInstance().nextId();
            ActiveOrderItem orderItem = new ActiveOrderItem(orderId,userId,eventId);
            saveOrder(orderItem);
            return new ActiveOrderDTO(orderItem);
        }
        else{
            throw new RuntimeException("the session has ended");
        }
    }

    @Override
    public void addSeatsToActiveOrder(SessionToken sessionToken, String orderId, String[] seatIds) {
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
    public List<BarcodeDTO> completeOrder2(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount, String orderId){
        if(!authenticationService.validate(sessionToken.getToken())){
            throw new RuntimeException("the session has ended");
        }
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if(order == null){
            throw new IllegalArgumentException("Order not found");
        }
        ActiveOrderDTO orderDTO = new ActiveOrderDTO(order);
        checkIfExpiredAndThrowException(order);
        //check purchasePolicy
        boolean upToPolicy = activeOrderPublisher.publishIsUpToPolicy(orderDTO);
        boolean paymentResult = payment(paymentGateway, sessionToken, amount);
        if(!paymentResult){
            rollbackOrderReservations(order);
            activeOrderRepo.delete(orderId);
            throw new IllegalStateException("Payment failed");
        }
        List<BarcodeDTO> barcodesIssued = barCodeGateway.issueBarcodes(orderDTO);
        if(barcodesIssued == null){
            paymentGateway.refund(sessionToken.getToken(), amount, orderId);
            rollbackOrderReservations(order);
            activeOrderRepo.delete(orderId);
            throw new IllegalStateException("Barcode generation failed. Refund processed.");
        }
        activeOrderPublisher.publishCompletedOrder(orderDTO, amount);
        activeOrderRepo.delete(orderId);
        return barcodesIssued;
    }
    private void rollbackOrderReservations(ActiveOrderItem order) {
        // Unreserve specific seats
        if (order.getSeatIds() != null && !order.getSeatIds().isEmpty()) {
            // Convert List to Array if your publisher expects an array, otherwise pass the list
            String[] seatsArray = order.getSeatIds().toArray(new String[0]);
            activeOrderPublisher.publishUnreserveSeats(order.getEventId(), seatsArray);
        }

        // Unreserve standing area quantities
        if (order.getStandingAreaQuantities() != null && !order.getStandingAreaQuantities().isEmpty()) {
            for (HashMap.Entry<String, Integer> entry : order.getStandingAreaQuantities().entrySet()) {
                activeOrderPublisher.publishUnreserveStandingArea(order.getEventId(), entry.getKey(), entry.getValue());
            }
        }
    }
    @Override
    public void completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount, String orderId)
    {
        if(authenticationService.validate(sessionToken.getToken())){
            ActiveOrderItem order = activeOrderRepo.findById(orderId);
            if(order == null){
                throw new IllegalArgumentException("Order not found");
            }
            checkIfExpiredAndThrowException(order);
            
            //boolean paymentResult = activeOrderPublisher.publishPaymentEvent(paymentGateway, sessionToken, amount);
            boolean paymentResult = payment(paymentGateway, sessionToken, amount);
            if(paymentResult) {
                //add to history order repo and delete from active order repo
                
                activeOrderRepo.delete(orderId);
            }
            else
            {
                activeOrderPublisher.publishUnreserveTickets(order.getEventId(), order.getQuantity());
                activeOrderRepo.delete(orderId);
                throw new IllegalStateException("Payment failed");
            }
        }
        else{
            throw new RuntimeException("the session has ended");
        }
    }
    //TODO: we may need to publish an event about the succesful purchase so that eventService can update the number of tickets sold for the event,but we can do that in the completeOrder method after the payment is successful

    public boolean payment(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount) {
        // TODO Auto-generated method stub
        if(authenticationService.validate(sessionToken.getToken())) {
            return paymentGateway.pay(); // Placeholder return value, replace with actual payment processing login
            //throw new UnsupportedOperationException("Unimplemented method 'payment'");
        }else{
            throw new RuntimeException("the session has ended");
        }
    }


    @Override
    public void updateActiveOrder(SessionToken sessionToken, String orderId, int newQuantity) 
    {
        if(authenticationService.validate(sessionToken.getToken())) {
            ActiveOrderItem order = activeOrderRepo.findById(orderId);
            if (order == null) {
                throw new IllegalArgumentException("Order not found");
            }
            checkIfExpiredAndThrowException(order);
            if (newQuantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }

            if (newQuantity == order.getQuantity()) {
                return;
            }
            if (newQuantity < order.getQuantity()) {
                activeOrderPublisher.publishUnreserveTickets(order.getEventId(), order.getQuantity() - newQuantity);
                order.setQuantity(newQuantity);
                activeOrderRepo.update(order);
                return;
            } else {
                boolean reserved = activeOrderPublisher.publishReserveTickets(order.getEventId(), newQuantity - order.getQuantity());
                if (!reserved) {
                    throw new IllegalStateException("Not enough tickets available");
                }
                order.setQuantity(newQuantity);
                activeOrderRepo.update(order);
            }
        }else{
            throw new RuntimeException("the session is over");
        }
    }
   

    
    private void checkIfExpiredAndThrowException(ActiveOrderItem order){
        if(order.getCreatedAt().getTime() + ActiveOrderItem.EXPIRATION_TIME_MINUTES*60*1000 < System.currentTimeMillis())
        {
            activeOrderPublisher.publishUnreserveTickets(order.getEventId(), order.getQuantity());
            activeOrderRepo.delete(order.getOrderId());
            throw new IllegalStateException("Order has expired");
        }
    }
    @Override
    public boolean saveOrder(ActiveOrderItem order) {
        try{
            if(order == null){
                System.out.println("Order cannot be null");
                return false;
            }
       
            if(!isValidEventID(order.getEventId()) || !isValidOrderID(order.getOrderId())) {
                System.out.println("bad order ID or event ID");
                return false;
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
