package com.ticketpurchasingsystem.project.domain.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class UserProductionTests {

    @Test
    void GivenProductionRole_WhenAddProduction_ThenRoleStored() {
        UserProduction production = new UserProduction();

        production.addProduction(1, UserProduction.RoleInProduction.OWNER);

        assertEquals(UserProduction.RoleInProduction.OWNER, production.getProductionRole(1));
    }

    @Test
    void GivenMissingProduction_WhenGetProductionRole_ThenThrowIllegalArgumentException() {
        UserProduction production = new UserProduction();

        assertThrows(IllegalArgumentException.class, () -> production.getProductionRole(999));
    }

    @Test
    void GivenExistingProduction_WhenRemoveProduction_ThenRemoved() {
        UserProduction production = new UserProduction();
        production.addProduction(5, UserProduction.RoleInProduction.MANAGER);

        production.removeProduction(5);

        assertThrows(IllegalArgumentException.class, () -> production.getProductionRole(5));
    }

    @Test
    void GivenMissingProduction_WhenRemoveProduction_ThenThrowIllegalArgumentException() {
        UserProduction production = new UserProduction();

        assertThrows(IllegalArgumentException.class, () -> production.removeProduction(5));
    }

    @Test
    void GivenProductions_WhenGetAllProductions_ThenReturnCopy() {
        UserProduction production = new UserProduction();
        production.addProduction(1, UserProduction.RoleInProduction.FOUNDER);

        Map<Integer, UserProduction.RoleInProduction> copy = production.getAllProductions();
        copy.put(2, UserProduction.RoleInProduction.MANAGER);

        assertTrue(production.getAllProductions().containsKey(1));
        assertFalse(production.getAllProductions().containsKey(2));
    }
}
