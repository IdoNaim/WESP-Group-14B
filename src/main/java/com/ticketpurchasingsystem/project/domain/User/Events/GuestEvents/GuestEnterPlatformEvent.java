package com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;;

public class GuestEnterPlatformEvent extends GuestEvents {

    private String guestId;
    private String sessionTokenStr;

    public GuestEnterPlatformEvent(String guestId, String sessionTokenStr) {
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
