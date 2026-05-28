package com.ticketpurchasingsystem.project.domain.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UserInfoTests {

    @Test
    void GivenMemberUser_WhenConstructed_ThenDefaultsSet() {
        UserInfo user = new UserInfo("user-1", "Itai", "itai@test.com", "secret", UserGroupDiscount.STUDENT);

        assertEquals("user-1", user.getId());
        assertEquals("Itai", user.getName());
        assertEquals("itai@test.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals(UserState.MEMBER, user.getUserState());
        assertEquals(UserGroupDiscount.STUDENT, user.getUserGroupDiscount());
        assertFalse(user.isLoggedIn());
        assertNull(user.getSessionTokenStr());
        assertNotNull(user.getUserProduction());
        assertFalse(user.isGuest());
    }

    @Test
    void GivenGuestUser_WhenConstructed_ThenDefaultsSet() {
        UserInfo guest = new UserInfo("guest-1", "token-1");

        assertEquals("guest-1", guest.getId());
        assertEquals("", guest.getName());
        assertEquals("", guest.getEmail());
        assertEquals("", guest.getPassword());
        assertEquals(UserState.GUEST, guest.getUserState());
        assertEquals(UserGroupDiscount.NONE, guest.getUserGroupDiscount());
        assertFalse(guest.isLoggedIn());
        assertEquals("token-1", guest.getSessionTokenStr());
        assertNull(guest.getUserProduction());
        assertTrue(guest.isGuest());
    }

    @Test
    void GivenLoggedOutUser_WhenSetUserGroupDiscount_ThenThrowIllegalStateException() {
        UserInfo user = new UserInfo("user-1", "Eden", "eden@test.com", "secret", UserGroupDiscount.NONE);

        assertThrows(IllegalStateException.class, () -> user.setUserGroupDiscount(UserGroupDiscount.SENIOR));
    }

    @Test
    void GivenLoggedInUser_WhenSetUserGroupDiscountWithNull_ThenSetToNone() {
        UserInfo user = new UserInfo("user-1", "Eden", "eden@test.com", "secret", UserGroupDiscount.STUDENT);
        user.setLoggedIn(true);

        user.setUserGroupDiscount(null);

        assertEquals(UserGroupDiscount.NONE, user.getUserGroupDiscount());
    }

    @Test
    void GivenValidStateStrings_WhenSetState_ThenStateUpdated() {
        UserInfo user = new UserInfo("user-1", "Eden", "eden@test.com", "secret", UserGroupDiscount.NONE);

        user.setState("guest");
        assertEquals(UserState.GUEST, user.getUserState());
        assertEquals("GUEST", user.getState());

        user.setState("member");
        assertEquals(UserState.MEMBER, user.getUserState());
        assertEquals("MEMBER", user.getState());
    }

    @Test
    void GivenInvalidState_WhenSetState_ThenThrowIllegalArgumentException() {
        UserInfo user = new UserInfo("user-1", "Eden", "eden@test.com", "secret", UserGroupDiscount.NONE);

        assertThrows(IllegalArgumentException.class, () -> user.setState("admin"));
    }

    @Test
    void GivenUser_WhenSettersCalled_ThenValuesUpdated() {
        UserInfo user = new UserInfo("user-1", "Eden", "eden@test.com", "secret", UserGroupDiscount.NONE);
        UserProduction production = new UserProduction();

        user.setId("user-2");
        user.setName("Itay");
        user.setEmail("itay@test.com");
        user.setPassword("new-secret");
        user.setUserState(UserState.GUEST);
        user.setSessionTokenStr("token-2");
        user.setUserProduction(production);
        user.setLoggedIn(true);

        assertEquals("user-2", user.getId());
        assertEquals("Itay", user.getName());
        assertEquals("itay@test.com", user.getEmail());
        assertEquals("new-secret", user.getPassword());
        assertEquals(UserState.GUEST, user.getUserState());
        assertEquals("token-2", user.getSessionTokenStr());
        assertEquals(production, user.getUserProduction());
        assertTrue(user.isLoggedIn());
    }
}
