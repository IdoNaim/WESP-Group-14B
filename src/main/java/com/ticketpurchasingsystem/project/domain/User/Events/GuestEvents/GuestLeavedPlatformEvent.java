package com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents;


public class GuestLeavedPlatformEvent extends GuestEvents {
    private String guestId;
    private String sessionTokenStr;

    public GuestLeavedPlatformEvent(String guestId, String sessionTokenStr) {
        this.guestId = guestId;
        this.sessionTokenStr = sessionTokenStr;
    }

    @Override
    public String getSessionToken() {
        return sessionTokenStr;
    }

    public String getGuestId() {
        return guestId;
    }

}
