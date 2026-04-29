package com.ticketpurchasingsystem.project.domain.User.Events.GuestEvents;

import com.ticketpurchasingsystem.project.domain.User.Events.Events;

public abstract class GuestEvents extends Events {

    public abstract String getSessionToken();
}
