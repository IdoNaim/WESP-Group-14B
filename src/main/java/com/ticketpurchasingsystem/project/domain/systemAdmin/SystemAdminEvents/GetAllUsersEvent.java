package com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents;

import com.ticketpurchasingsystem.project.domain.User.UserDTO;

public class GetAllUsersEvent extends GetAllEvent<UserDTO> {
    public GetAllUsersEvent(String reqId) {
        super(reqId);
    }

}
