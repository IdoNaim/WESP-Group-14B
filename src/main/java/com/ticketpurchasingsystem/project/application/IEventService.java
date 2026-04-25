package com.ticketpurchasingsystem.project.application;

import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import java.time.LocalDateTime;
import java.util.List;
import com.ticketpurchasingsystem.project.domain.event.SeatingMap;



public interface IEventService {
     public boolean createEvent(EventDTO eventDTO, PurchasePolicyDTO purchasePolicyDTO, List<DiscountDTO> discountPolicyDTO);
     public EventDTO searchEvent(int eventId);
     public List<EventDTO> searchEventsByCompany(int companyId);
     public boolean editEventDate(int eventId, LocalDateTime newDateTime);
     public boolean removeEvent(int eventId);
     public boolean editEventInventory(int eventId, int newCapacity);
     public boolean configureEventSeatinMap(int eventId, SeatingMap seatingMapDTO);
}
