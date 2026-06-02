package com.ticketpurchasingsystem.project.domain.event.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import com.ticketpurchasingsystem.project.domain.Utils.AssignedSeatDTO;
import com.ticketpurchasingsystem.project.domain.Utils.SeatingMapDTO;
import com.ticketpurchasingsystem.project.domain.Utils.StandingAreaDTO;
public class SeatingMap {
    private ConcurrentMap<String,AssignedSeat> seats;
    private ConcurrentMap<String, StandingArea> standingAreas;
    //private HashMap<String, Bookable> PurchaseAreas;
    private final AtomicLong areaIDGenerator = new AtomicLong(0);

    public SeatingMap() {
        this.seats = new ConcurrentHashMap<>();
        this.standingAreas = new ConcurrentHashMap<>();
    }

    private String generateAreaID() {
        return "" + areaIDGenerator.getAndIncrement();
    }

    public boolean addStandingArea(int capacity, double priceForTicket){
        if(capacity <= 0 || priceForTicket < 0){
            return false;
        }
        String areaID = generateAreaID();
        standingAreas.put(areaID, new StandingArea(capacity, priceForTicket, areaID));
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
                seats.put(seatID, seat);
            }
        }
        return true;
    }

    public boolean bookStandingArea(String areaID, String orderId, int numberOfTickets){
        if(!standingAreas.containsKey(areaID)){
            return false;
        }
        StandingArea area = standingAreas.get(areaID);
        return area.book(orderId, numberOfTickets);
    }

    public boolean bookAssignedSeats(List<String> seatIDs, String orderId){
        for(String seatID : seatIDs){
            if(!seats.containsKey(seatID)){
                return false;
            }
            AssignedSeat seat = seats.get(seatID);
            if(!seat.book(orderId, 1)){
                for(String bookedSeatID : seatIDs){
                    if(bookedSeatID.equals(seatID)){
                        break;
                    }
                    AssignedSeat bookedSeat = seats.get(bookedSeatID);
                    bookedSeat.unbook(1);
                }
                throw new IllegalStateException("Failed to book all seats. Rolled back successfully booked seats.");
            }
        }
        return true;
    }

    public boolean unbookStandingArea(String areaID, int numberOfTickets){
        if(!standingAreas.containsKey(areaID)){
            return false;
        }
        StandingArea area = standingAreas.get(areaID);
        return area.unbook(numberOfTickets);
    }

    public boolean unbookAssignedSeats(List<String> seatIDs){
        StringBuilder failedUnbookSeats = new StringBuilder();
        for(String seatID : seatIDs){
            if(!seats.containsKey(seatID)){
                throw new IllegalArgumentException("failed to unbook Seats, got seats that dont exist");
            }
            AssignedSeat seat = seats.get(seatID);
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
        if(standingAreas.containsKey(areaID)){
            standingAreas.remove(areaID);
            return true;
        }
        if(seats.containsKey(areaID)){
            seats.remove(areaID);
            return true;
        }
        return false;
    }

    public boolean editPriceForTicket(String areaID, double newPrice){
        if(newPrice < 0){
            return false;
        }
        if(standingAreas.containsKey(areaID)){
            standingAreas.get(areaID).setPriceForTicket(newPrice);
            return true;
        }
        if(seats.containsKey(areaID)){
            seats.get(areaID).setPriceForTicket(newPrice);
            return true;
        }
        return false;
    }

    public double getPriceForTicket(String areaID){
        if(standingAreas.containsKey(areaID)){
            return standingAreas.get(areaID).getPriceForTicket();
        }
        if(seats.containsKey(areaID)){
            return seats.get(areaID).getPriceForTicket();
        }
        return -1;
    }

    public AssignedSeat getSeat(String seatID){
        return seats.get(seatID);
    }

    public StandingArea getArea(String areaID){
        return standingAreas.get(areaID);
    }

    public List<String> getSeatIds(){
        return new ArrayList<>(seats.keySet());
    }

    public List<String> getAreaIds(){
        return new ArrayList<>(standingAreas.keySet());
    }

    public Map<String, Bookable> getPurchaseAreas(){
        Map<String, Bookable> combined = new HashMap<>(seats);
        combined.putAll(standingAreas);
        return combined;
    }
    public SeatingMapDTO getDTO(){
        List<AssignedSeatDTO> assignedSeatsDTO = new ArrayList<>();
        for(AssignedSeat seat : seats.values()){
            if(seat != null)
                assignedSeatsDTO.add(new AssignedSeatDTO(seat.getId(), seat.isBooked(), seat.getOrderId(), seat.getPriceForTicket()));
        }
        List<StandingAreaDTO> standingAreasDTO = new ArrayList<>();
        for(StandingArea area : standingAreas.values()){
            if(area != null)
                standingAreasDTO.add(new StandingAreaDTO(area.getId(), area.getAvalibleSeatNumber(), area.getCapacity(), area.getPriceForTicket()));
        }
        return new SeatingMapDTO(assignedSeatsDTO, standingAreasDTO);
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
