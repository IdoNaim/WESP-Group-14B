package com.ticketpurchasingsystem.project.domain.Utils;

import java.util.List;

public class SeatingMapDTO {
    private List<AssignedSeatDTO> assignedSeats;
    private List<StandingAreaDTO> standingAreas;

    public SeatingMapDTO(List<AssignedSeatDTO> assignedSeats, List<StandingAreaDTO> standingAreas) {
        this.assignedSeats = assignedSeats;
        this.standingAreas = standingAreas;
    }
    public List<AssignedSeatDTO> getAssignedSeats() {
        return assignedSeats;
    }
    public List<StandingAreaDTO> getStandingAreas() {
        return standingAreas;
    }
    public void setAssignedSeats(List<AssignedSeatDTO> assignedSeats) {
        this.assignedSeats = assignedSeats;
    }
    public void setStandingAreas(List<StandingAreaDTO> standingAreas) {
        this.standingAreas = standingAreas;
    }
    
}