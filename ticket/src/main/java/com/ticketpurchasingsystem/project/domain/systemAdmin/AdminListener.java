package com.ticketpurchasingsystem.project.domain.systemAdmin;

import com.ticketpurchasingsystem.project.application.ISystemAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class AdminListener implements ApplicationListener<ApplicationEvent> {

    @Autowired
    private ISystemAdminService systemAdminService;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }
}
