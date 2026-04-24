package com.ticketpurchasingsystem.project.domain.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ticketpurchasingsystem.project.domain.systemAdmin.SystemAdminEvents.GetAllUsersEvent;

public class UserListenerTests {
    String adminId = "admin-123";

    @Test
    void WhenAskForUserGivenUserThenReceiveUser() {
        IUserRepo repo = mock(IUserRepo.class);
        List<UserInfo> users = List.of(new UserInfo("Tomer", "KingTomer@walla.com", "pass"));
        when(repo.findAll()).thenReturn(users);

        UserListener listener = new UserListener(repo);
        GetAllUsersEvent event = new GetAllUsersEvent(this);
        listener.onApplicationEvent(event);

        assertEquals(users, event.getResult(adminId));
    }

    @Test
    void WhenAskForUsersGivenEmptyRepoThenReceiveEmptyList() {
        IUserRepo repo = mock(IUserRepo.class);
        when(repo.findAll()).thenReturn(List.of());

        UserListener listener = new UserListener(repo);
        GetAllUsersEvent event = new GetAllUsersEvent(this);
        listener.onApplicationEvent(event);

        assertNotNull(event.getResult(adminId));
        assertTrue(event.getResult(adminId).isEmpty());
    }

   
}
