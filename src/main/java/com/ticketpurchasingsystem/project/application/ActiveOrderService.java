package com.ticketpurchasingsystem.project.application;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.*;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderEvents.IsValidEventIDEvent;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

import com.ticketpurchasingsystem.project.domain.Utils.IdGenerator;



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

    @Override
    public void completeActiveOrder(SessionToken sessionToken, String userId, String eventId, int quantity) {
        boolean reserved = activeOrderPublisher.publishReserveTickets(eventId, quantity);

        
    }

    @Override
    public void updateActiveOrder(String orderId, int quantity) {
        // TODO Auto-generated method stub
        
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
