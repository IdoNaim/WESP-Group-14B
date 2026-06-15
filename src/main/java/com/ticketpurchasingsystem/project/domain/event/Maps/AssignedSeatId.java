package com.ticketpurchasingsystem.project.domain.event.Maps;

import java.io.Serializable;
import java.util.Objects;

public class AssignedSeatId implements Serializable {

    private String eventId;
    private String seatId;

    // JPA requires a default, no-args constructor
    public AssignedSeatId() {}

    public AssignedSeatId(String eventId, String seatId) {
        this.eventId = eventId;
        this.seatId = seatId;
    }

    // equals() and hashCode() are MANDATORY for composite keys to work correctly in Hibernate
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignedSeatId that = (AssignedSeatId) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(seatId, that.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, seatId);
    }
}