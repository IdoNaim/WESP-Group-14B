package com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents;

import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public class GuestLeavePlatformEvent extends GuestEvents {
    private SessionToken sessionToken;

    public GuestLeavePlatformEvent(SessionToken sessionToken) {
        this.sessionToken = sessionToken;
    }

    @Override
    public SessionToken getSessionToken() {
        return sessionToken;
    }
    
}
