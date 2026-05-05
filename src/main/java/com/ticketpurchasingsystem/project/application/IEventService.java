package com.ticketpurchasingsystem.project.application;

import java.time.LocalDateTime;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.SeatingMap;



public interface IEventService {
     public boolean createEvent(EventDTO eventDTO, PurchasePolicyDTO purchasePolicyDTO, List<DiscountDTO> discountPolicyDTO);
     public EventDTO searchEvent(String eventId);
     public List<EventDTO> searchEventsByCompany(int companyId);
     public boolean editEventDate(String eventId, LocalDateTime newDateTime);
     public boolean removeEvent(String eventId);
     public boolean editEventInventory(String eventId, int newCapacity);
     public SeatingMap configureSeatingMap(List<SeatingAreaConfig> seatingAreas, List<SeatingAreaConfig> standingAreas);
     //public boolean configureEventSeatinMap(String eventId, SeatingMap seatingMapDTO);
}
