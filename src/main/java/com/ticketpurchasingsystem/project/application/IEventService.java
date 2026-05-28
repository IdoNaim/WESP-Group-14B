package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;
import com.ticketpurchasingsystem.project.domain.event.Maps.StandingAreaConfig;


public interface IEventService {
     public boolean createEvent(String sessionToken, EventDTO eventDTO, PurchasePolicyDTO purchasePolicyDTO, List<DiscountDTO> discountPolicyDTO);
     public EventDTO searchEvent(String sessionToken, String eventId);
     public List<EventDTO> searchEventsByCompany(String sessionToken, int companyId);
     public boolean editEventDate(String sessionToken, String eventId, LocalDateTime newDateTime);
     public boolean removeEvent(String sessionToken, String eventId);
     public boolean editEventInventory(String sessionToken, String eventId, int newCapacity);
     public SeatingMap configureSeatingMap(String sessionToken, List<SeatingAreaConfig> seatingAreas, List<StandingAreaConfig> standingAreas);
     public boolean editEventSeatingMap(String sessionToken, String eventId, SeatingMap seatingMap);
     //public boolean configureEventSeatinMap(String eventId, SeatingMap seatingMapDTO);
     public void releaseSeats(String sessionToken, String orderId, String eventId, List<String> seatIds);
     public void releaseStandingArea(String sessionToken, String eventId, String areaID, int quantity);
     public boolean reserveSeats(String sessionToken, String orderId, String eventId, List<String> seatIds);
     public boolean reserveStandingArea(String sessionToken, String eventId, String areaId, int quantity);
     public boolean checkSeatAvailability(String eventId, List<String> seatIds);
     public boolean checkStandingAreaAvailability(String eventId, String areaId, int quantity);
     public List<String> checkSeatsReserved(String sessionToken, String orderId, String eventId, List<String> seatIds);
     public boolean editEventPurchasePolicy(String sessionToken, String eventId, PurchasePolicyDTO purchasePolicyDTO);
}
