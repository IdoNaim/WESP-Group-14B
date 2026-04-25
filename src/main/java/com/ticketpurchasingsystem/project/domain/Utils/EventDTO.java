package com.ticketpurchasingsystem.project.domain.Utils;
import java.time.LocalDateTime;

public record EventDTO(
    Integer companyId,
    String eventName,
    Integer eventCapacity,
    LocalDateTime eventDateTime,
    Boolean isActive
) {}


