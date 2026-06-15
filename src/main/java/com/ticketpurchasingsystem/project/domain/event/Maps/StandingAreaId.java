package com.ticketpurchasingsystem.project.domain.event.Maps;

import java.io.Serializable;
import java.util.Objects;

public class StandingAreaId implements Serializable {

    private String eventId;
    private String areaId;

    // JPA Required no-arg constructor
    public StandingAreaId() {}

    public StandingAreaId(String eventId, String areaId) {
        this.eventId = eventId;
        this.areaId = areaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandingAreaId that = (StandingAreaId) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(areaId, that.areaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, areaId);
    }
}