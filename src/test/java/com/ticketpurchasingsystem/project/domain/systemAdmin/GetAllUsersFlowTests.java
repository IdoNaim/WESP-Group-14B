package com.ticketpurchasingsystem.project.domain.systemAdmin;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.ApplicationEventPublisher;

import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllUsersEvent;

public class GetAllUsersFlowTests {
    String adminId = "admin-123";

    // ---- GetAllUsersEvent ----

    @Test
    void WhenGetResultGivenResultWasSetThenReturnResult() {
        GetAllUsersEvent event = new GetAllUsersEvent(this);
        List<UserInfo> users = List.of(new UserInfo("Alice", "a@b.com", "pass"));
        event.setResult(users);
        assertEquals(users, event.getResult(adminId));
    }

    @Test
    void WhenGetResultGivenNoResultSetThenReturnNull() {
        GetAllUsersEvent event = new GetAllUsersEvent(this);
        assertNull(event.getResult(adminId));
    }

    // ---- AdminPublisher ----

    @Test
    void WhenPublishGetAllUsersGivenListenerRespondshenReturnUsers() {
        List<UserInfo> mockUsers = List.of(new UserInfo("Bob", "b@c.com", "pass"));
        ApplicationEventPublisher springPublisher = mock(ApplicationEventPublisher.class);

        doAnswer(invocation -> {
            GetAllUsersEvent event = invocation.getArgument(0);
            event.setResult(mockUsers);
            return null;
        }).when(springPublisher).publishEvent(any(GetAllUsersEvent.class));

        AdminPublisher publisher = new AdminPublisher(springPublisher);
        List<UserInfo> result = publisher.publishGetAllUsers(adminId);

        assertEquals(mockUsers, result);
    }

    @Test
    void WhenPublishGetAllUsersGivenNoListenerThenReturnNull() {
        ApplicationEventPublisher springPublisher = mock(ApplicationEventPublisher.class);
        AdminPublisher publisher = new AdminPublisher(springPublisher);

        List<UserInfo> result = publisher.publishGetAllUsers(adminId);

        assertNull(result);
    }

    @Test
    void WhenPublishGetAllUsersGivenAdminIdThenEventIsPublished() {
        ApplicationEventPublisher springPublisher = mock(ApplicationEventPublisher.class);
        AdminPublisher publisher = new AdminPublisher(springPublisher);

        publisher.publishGetAllUsers(adminId);

        verify(springPublisher).publishEvent(any(GetAllUsersEvent.class));
    }

    // ---- SystemAdmin ----

    @Test
    void WhenGetUsersInfoGivenPublisherReturnsThenReturnUsers() {
        AdminPublisher publisher = mock(AdminPublisher.class);
        AdminInfo adminInfo = new AdminInfo("admin", "admin@test.com");
        List<UserInfo> mockUsers = List.of(new UserInfo("Alice", "a@b.com", "pass"));


        when(publisher.publishGetAllUsers(adminInfo.getId())).thenReturn(mockUsers);

        SystemAdmin admin = new SystemAdmin(adminInfo, publisher);
        List<UserInfo> result = admin.getUsersInfo();

        assertEquals(mockUsers, result);
        verify(publisher).publishGetAllUsers(adminInfo.getId());
    }

    @Test
    void WhenGetUsersInfoGivenAdminThenPublisherCalledWithAdminId() {
        AdminPublisher publisher = mock(AdminPublisher.class);
        AdminInfo adminInfo = new AdminInfo("admin", "admin@test.com");

        when(publisher.publishGetAllUsers(anyString())).thenReturn(List.of());
        new SystemAdmin(adminInfo, publisher).getUsersInfo();

        verify(publisher).publishGetAllUsers(adminInfo.getId());
    }
}
