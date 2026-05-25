package com.ticketpurchasingsystem.project.domain.User;

import com.ticketpurchasingsystem.project.domain.User.Events.Events;

public record ExitProcessData(UserInfo userInfoToStore, Events exitEvent) {
}
