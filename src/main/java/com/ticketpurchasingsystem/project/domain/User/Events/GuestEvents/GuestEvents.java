package com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents;

import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;

public abstract class GuestEvents extends Events {
    private SessionToken sessionToken;

    public abstract SessionToken getSessionToken();
}
