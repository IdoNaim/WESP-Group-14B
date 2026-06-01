package com.ticketpurchasingsystem.project.domain.Utils;
import java.time.LocalDateTime;

public record EventDTO(
        String eventId,
        Integer companyId,
        String eventName,
        Integer eventCapacity,
        LocalDateTime eventDateTime,
        Boolean isActive,
        String location,
        Double ticketPrice
) {
    public EventDTO(String eventId, Integer companyId, String eventName,
                    Integer eventCapacity, LocalDateTime eventDateTime, Boolean isActive) {
        this(eventId, companyId, eventName, eventCapacity, eventDateTime, isActive, null, null);
    }
}
