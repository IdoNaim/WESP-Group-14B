package com.ticketpurchasingsystem.project.domain.event;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
public class SeatingMap {
    private HashMap<String, Bookable> PurchaseAreas;
    private final AtomicLong areaIDGenerator = new AtomicLong(0);

    public SeatingMap() {
        this.PurchaseAreas = new HashMap<>();
    }
    private String generateAreaID() {
        return "" + areaIDGenerator.getAndIncrement();
    }
    //TODO: add name
    public boolean addStandingArea(int capacity, double priceForTicket){
        if(capacity <= 0 || priceForTicket < 0){
            return false;
        }
        String areaID = generateAreaID();
        PurchaseAreas.put(areaID, new StandingArea(capacity, priceForTicket, areaID));
        return true;
    }

    public boolean addSeatingArea(int rows, int seatsPerRow, double priceForTicket){
        if(rows <= 0 || seatsPerRow <= 0 || priceForTicket < 0){
            return false;
        }
        String areaID = generateAreaID();
        for(int i = 1; i <= rows; i++){
            for(int j = 1; j <= seatsPerRow; j++){
                AssignedSeat seat = new AssignedSeat(areaID, i, j, priceForTicket);
                String seatID = seat.getId();
                PurchaseAreas.put(seatID, seat);
            }
        }
        return true;
    }

    public boolean bookStandingArea(String areaID, String orderId, int numberOfTickets){
        if(!PurchaseAreas.containsKey(areaID)){
            return false;
        }
        Bookable area = PurchaseAreas.get(areaID);
        return area.book(orderId, numberOfTickets);
    }

    public boolean bookAssignedSeats(List<String> seatIDs, String orderId){
        for(String seatID : seatIDs){
            if(!PurchaseAreas.containsKey(seatID)){
                return false;
            }
            Bookable seat = PurchaseAreas.get(seatID);
            if(!seat.book(orderId, 1)){
                for(String bookedSeatID : seatIDs){
                    if(bookedSeatID.equals(seatID)){
                        break;
                    }
                    Bookable bookedSeat = PurchaseAreas.get(bookedSeatID);
                    bookedSeat.unbook(1);
                }
                throw new IllegalStateException("Failed to book all seats. Rolled back successfully booked seats.");
            }
        }
        return true;
    }
    
    public boolean unbookStandingArea(String areaID, int numberOfTickets){
        if(!PurchaseAreas.containsKey(areaID)){
            return false;
        }
        Bookable area = PurchaseAreas.get(areaID);
        return area.unbook(numberOfTickets);
    }


    //
    public boolean unbookAssignedSeats(List<String> seatIDs){
        StringBuilder failedUnbookSeats = new StringBuilder();
        for(String seatID : seatIDs){
            if(!PurchaseAreas.containsKey(seatID)){
                throw new IllegalArgumentException("failed to unbook Seats, got seats that dont exist");
            }
            Bookable seat = PurchaseAreas.get(seatID);
            if(!seat.unbook(1)){
                failedUnbookSeats.append(seatID).append(", ");
            }
        }
        if (failedUnbookSeats.length() > 0) {
            throw new IllegalStateException("Failed to unbook seats: " + failedUnbookSeats.substring(0, failedUnbookSeats.length() - 2));
        }
        return true;
    }

    public boolean removeArea(String areaID){
        if(PurchaseAreas.containsKey(areaID)){
            PurchaseAreas.remove(areaID);
            return true;
        }
        return false;
    }
    public boolean editPriceForTicket(String areaID, double newPrice){
        if(newPrice < 0 || !PurchaseAreas.containsKey(areaID)){
            return false;
        }
        Bookable area = PurchaseAreas.get(areaID);
        area.setPriceForTicket(newPrice);
        return true;
    }
    public double getPriceForTicket(String areaID){
        if(!PurchaseAreas.containsKey(areaID)){
            return -1;
        }
        Bookable area = PurchaseAreas.get(areaID);
        return area.getPriceForTicket();
    }
    
    public Bookable getArea(String areaID){
        return PurchaseAreas.get(areaID);
    }

    public HashMap<String, Bookable> getPurchaseAreas() {
        return PurchaseAreas;
    }
    
    // public boolean addAssignedSeat(String zone, int row, int number, double priceForTicket){
    //     if(row <= 0 || number <= 0 || priceForTicket <= 0){
    //         return false;

    //     }
    //     AssignedSeat seat = new AssignedSeat(zone, row, number, priceForTicket);
    //     String seatID = seat.getId();
    //     PurchaseAreas.put(seatID, seat);
    //     return true;
    // }
}
