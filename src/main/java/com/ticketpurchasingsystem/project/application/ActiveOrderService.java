package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsValidEventIDEvent;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

import com.ticketpurchasingsystem.project.domain.Utils.IdGenerator;
import com.ticketpurchasingsystem.project.application.IPaymentGateway;



public class ActiveOrderService implements IActiveOrderService {
    ActiveOrderListener activeOrderListener;
    ActiveOrderPublisher activeOrderPublisher;
    IActiveOrderRepo activeOrderRepo;
    //AuthenticationService authenticationService;
    public ActiveOrderService(ActiveOrderListener activeOrderListener, ActiveOrderPublisher activeOrderPublisher, IActiveOrderRepo activeOrderRepo) {
        this.activeOrderListener = activeOrderListener;
        this.activeOrderPublisher = activeOrderPublisher;
        this.activeOrderRepo = activeOrderRepo;
//        this.authenticationService = authenticationService;
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
    @Override
    public ActiveOrderItem createPendingOrder(SessionToken sessionToken, String userId, String eventId, int quantity) {
        boolean reserved = activeOrderPublisher.publishReserveTickets(eventId, quantity);
        if(!reserved){
            return null;
        }
        String orderId = ""+ IdGenerator.getInstance().nextId();
        ActiveOrderItem orderItem = new ActiveOrderItem(orderId,userId,eventId, quantity);
        saveOrder(orderItem);
        return orderItem;
    }
    // gets paymentGateway because there multiple gateways each for a different payment method(paypal, bit...), so gets the payment method from UI after the user chose it
    @Override
    public void completeOrder(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount, String orderId)
    {
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if(order == null){
            throw new IllegalArgumentException("Order not found");
        }
        checkIfExpiredAndThrowException(order);
        
        //boolean paymentResult = activeOrderPublisher.publishPaymentEvent(paymentGateway, sessionToken, amount);
        boolean paymentResult = payment(paymentGateway, sessionToken, amount);
        if(paymentResult) {
            activeOrderRepo.delete(orderId);
        }
        else
        {
            activeOrderPublisher.publishUnreserveTickets(order.getEventId(), order.getQuantity());
            activeOrderRepo.delete(orderId);
            throw new IllegalStateException("Payment failed");
        }
    }
    //TODO: we may need to publish an event about the succesful purchase so that eventService can update the number of tickets sold for the event,but we can do that in the completeOrder method after the payment is successful

    public boolean payment(IPaymentGateway paymentGateway, SessionToken sessionToken, double amount) {
        // TODO Auto-generated method stub
        return true; // Placeholder return value, replace with actual payment processing logic
        //throw new UnsupportedOperationException("Unimplemented method 'payment'");
    }


    @Override
    public void updateActiveOrder(SessionToken sessionToken, String orderId, int newQuantity) 
    {
        ActiveOrderItem order = activeOrderRepo.findById(orderId);
        if(order == null){
            throw new IllegalArgumentException("Order not found");
        }
        checkIfExpiredAndThrowException(order);
        if(newQuantity <= 0){
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if(newQuantity == order.getQuantity()){
            return;
        }
        if(newQuantity < order.getQuantity()) {
            activeOrderPublisher.publishUnreserveTickets(order.getEventId(), order.getQuantity() - newQuantity);
            order.setQuantity(newQuantity);
            activeOrderRepo.update(order);
            return;
        }
        else{
            boolean reserved = activeOrderPublisher.publishReserveTickets(order.getEventId(), newQuantity - order.getQuantity());
            if(!reserved){
                throw new IllegalStateException("Not enough tickets available");
            }
            order.setQuantity(newQuantity); 
            activeOrderRepo.update(order);
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
            if(order.getQuantity() <= 0){
                System.out.println("Quantity must be greater than 0");
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
