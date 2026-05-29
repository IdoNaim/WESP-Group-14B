package com.ticketpurchasingsystem.project.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.application.UserService.UserApplicationListener;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AssignOwnerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.NewProdEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.AppointManagerEvent;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.User.UserProduction;

@ExtendWith(MockitoExtension.class)
class UserApplicationListenerTests {

    @Mock
    private UserService userService;

    private UserApplicationListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserApplicationListener(userService);
    }

    // onAddProductionRole
    @Test
    void GivenAssignOwnerEvent_WhenOnAddProductionRole_ThenAssignProductionRoleCalled() {
        ProductionCompany company = new ProductionCompany(new ProductionCompanyDTO("c", "d", "e"));
        company.setCompanyId(42);
        AssignOwnerEvent event = new AssignOwnerEvent(company, "appointer", "appointee");

        listener.onAddProductionRole(event);

        verify(userService, times(1)).assignProductionRole("appointee", 42, UserProduction.RoleInProduction.OWNER);
    }

    @Test
    void GivenUserIsNull_WhenOnAddProductionRole_ThenExceptionThrown() {
        ProductionCompany company = new ProductionCompany(new ProductionCompanyDTO("c", "d", "e"));
        company.setCompanyId(43);
        AssignOwnerEvent event = new AssignOwnerEvent(company, "a", null);

        doThrow(new RuntimeException("Appointee cannot be null")).when(userService).assignProductionRole(null, 43, UserProduction.RoleInProduction.OWNER);

        assertThrows(RuntimeException.class, () -> listener.onAddProductionRole(event));
    }

    // onNewProduction
    @Test
    void GivenNewProdCreated_WhenOnNewProduction_ThenAssignFounderCalled() {
        ProductionCompany company = new ProductionCompany(new ProductionCompanyDTO("c", "d", "e"));
        company.setCompanyId(11);
        company.setFounderId("founder1");
        NewProdEvent event = new NewProdEvent(company);

        listener.onNewProduction(event);

        verify(userService, times(1)).assignProductionRole("founder1", 11, UserProduction.RoleInProduction.FOUNDER);
    }

    @Test
    void GivenNewManagerAssigned_WhenOnNewProduction_ThenAssignManagerCalled() {
        ProductionCompany company = new ProductionCompany(new ProductionCompanyDTO("c", "d", "e"));
        company.setCompanyId(12);
        company.setFounderId("f1");
        company.appointManager("f1", "m1", Collections.emptySet());
        AppointManagerEvent event = new AppointManagerEvent(company, "f1", "m1", Collections.emptySet());

        listener.onAppointManagerEvent(event);

        verify(userService, times(1)).assignProductionRole("m1", 12, UserProduction.RoleInProduction.MANAGER);
    }

    // onIsUserRegistered
    @Test
    void GivenRegisteredUser_WhenOnIsUserRegistered_ThenReturnTrue() {
        IsUserRegisteredEvent event = new IsUserRegisteredEvent("u1");
        when(userService.isUserRegistered("u1")).thenReturn(true);

        listener.onIsUserRegistered(event);

        assertTrue(event.isRegistered());
        verify(userService, times(1)).isUserRegistered("u1");
    }

    @Test
    void GivenUnknownUser_WhenOnIsUserRegistered_ThenReturnFalse() {
        IsUserRegisteredEvent event = new IsUserRegisteredEvent("unknown");
        when(userService.isUserRegistered("unknown")).thenReturn(false);

        listener.onIsUserRegistered(event);

        assertFalse(event.isRegistered());
        verify(userService, times(1)).isUserRegistered("unknown");
    }

}
