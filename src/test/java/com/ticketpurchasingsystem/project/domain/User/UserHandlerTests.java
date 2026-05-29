package com.ticketpurchasingsystem.project.domain.User;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.domain.Utils.PasswordEncoderUtil;

public class UserHandlerTests {

    private UserHandler handler;

    private static final String USER_ID = "user-1";
    private static final String USERNAME = "Itai";
    private static final String EMAIL = "itai@test.com";
    private static final String PASSWORD = "pass123";
    private static final String TOKEN = "token-1";

    @BeforeEach
    void setUp() {
        handler = new UserHandler();
    }

    private UserInfo buildMemberUser() {
        return handler.registerUser(USER_ID, USERNAME, EMAIL, PASSWORD, UserGroupDiscount.STUDENT);
    }

    private UserInfo buildGuestUser() {
        return handler.handleGuestEntry(TOKEN, "guest-1");
    }

    @Test
    void GivenNullUser_WhenIsUserLoggedIn_ThenReturnFalse() {
        assertFalse(handler.isUserLoggedIn(null));
    }

    @Test
    void GivenLoggedInUser_WhenIsUserLoggedIn_ThenReturnTrue() {
        UserInfo user = buildMemberUser();
        user.setLoggedIn(true);

        assertTrue(handler.isUserLoggedIn(user));
    }

    @Test
    void GivenLoggedOutUser_WhenIsUserLoggedIn_ThenReturnFalse() {
        UserInfo user = buildMemberUser();
        user.setLoggedIn(false);

        assertFalse(handler.isUserLoggedIn(user));
    }

    @Test
    void GivenValidInput_WhenRegisterUser_ThenFieldsAndPasswordEncoded() {
        UserInfo user = buildMemberUser();

        assertNotNull(user);
        assertEquals(USER_ID, user.getId());
        assertEquals(USERNAME, user.getName());
        assertEquals(EMAIL, user.getEmail());
        assertFalse(user.isGuest());
        assertEquals(UserGroupDiscount.STUDENT, user.getUserGroupDiscount());
        assertNotEquals(PASSWORD, user.getPassword());
        assertTrue(PasswordEncoderUtil.matches(PASSWORD, user.getPassword()));
    }

    @Test
    void GivenInvalidEmail_WhenRegisterUser_ThenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () ->
            handler.registerUser(USER_ID, USERNAME, "invalid-email", PASSWORD, UserGroupDiscount.NONE)
        );
    }

    @Test
    void GivenGuestEntry_WhenHandleGuestEntry_ThenGuestInitialized() {
        UserInfo guest = buildGuestUser();

        assertNotNull(guest);
        assertTrue(guest.isGuest());
        assertEquals(TOKEN, guest.getSessionTokenStr());
        assertEquals(UserGroupDiscount.NONE, guest.getUserGroupDiscount());
        assertFalse(guest.isLoggedIn());
        assertNull(guest.getUserProduction());
    }

    @Test
    void GivenNullUser_WhenHandleUserExit_ThenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> handler.handleUserExit(null));
    }

    @Test
    void GivenMemberUser_WhenHandleUserExit_ThenClearsSessionAndLogin() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr("member-token");
        user.setLoggedIn(true);

        handler.handleUserExit(user);

        assertNull(user.getSessionTokenStr());
        assertFalse(user.isLoggedIn());
    }

    @Test
    void GivenMemberUser_WhenHandleUserExit_ThenSetterCallsInvoked() {
        UserInfo user = spy(buildMemberUser());
        user.setSessionTokenStr("member-token");
        user.setLoggedIn(true);

        handler.handleUserExit(user);

        verify(user).setSessionTokenStr(null);
        verify(user).setLoggedIn(false);
    }

    @Test
    void GivenGuestUser_WhenHandleUserExit_ThenNoChanges() {
        UserInfo guest = buildGuestUser();
        guest.setLoggedIn(true);
        guest.setSessionTokenStr("guest-token");

        handler.handleUserExit(guest);

        assertEquals("guest-token", guest.getSessionTokenStr());
        assertTrue(guest.isLoggedIn());
        assertTrue(guest.isGuest());
    }

    @Test
    void GivenGuestUser_WhenHandleUserExit_ThenNoSetterCalls() {
        UserInfo guest = spy(buildGuestUser());
        guest.setLoggedIn(true);
        guest.setSessionTokenStr("guest-token");

        handler.handleUserExit(guest);

        verify(guest, never()).setSessionTokenStr(null);
        verify(guest, never()).setLoggedIn(false);
    }

    @Test
    void GivenValidCredentials_WhenLoginUser_ThenLoginSucceeds() {
        UserInfo user = buildMemberUser();

        handler.loginUser(user, PASSWORD, "new-token");

        assertTrue(user.isLoggedIn());
        assertEquals("new-token", user.getSessionTokenStr());
    }

    @Test
    void GivenValidCredentials_WhenLoginUser_ThenSetterCallsInvoked() {
        UserInfo user = spy(buildMemberUser());

        handler.loginUser(user, PASSWORD, "new-token");

        verify(user).setSessionTokenStr("new-token");
        verify(user).setLoggedIn(true);
    }

    @Test
    void GivenWrongPassword_WhenLoginUser_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();

        assertThrows(RuntimeException.class, () ->
            handler.loginUser(user, "wrong-pass", "token")
        );
    }

    @Test
    void GivenAlreadyLoggedInUser_WhenLoginUser_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setLoggedIn(true);

        assertThrows(RuntimeException.class, () ->
            handler.loginUser(user, PASSWORD, "token")
        );
    }

    @Test
    void GivenNullUser_WhenLoginUser_ThenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () ->
            handler.loginUser(null, PASSWORD, "token")
        );
    }

    @Test
    void GivenLoggedInUser_WhenLogoutUser_ThenLogoutSucceeds() {
        UserInfo user = buildMemberUser();
        user.setLoggedIn(true);
        user.setSessionTokenStr("token");

        handler.logoutUser(user);

        assertFalse(user.isLoggedIn());
        assertNull(user.getSessionTokenStr());
    }

    @Test
    void GivenLoggedInUser_WhenLogoutUser_ThenSetterCallsInvoked() {
        UserInfo user = spy(buildMemberUser());
        user.setLoggedIn(true);
        user.setSessionTokenStr("token");

        handler.logoutUser(user);

        verify(user).setLoggedIn(false);
        verify(user).setSessionTokenStr(null);
    }

    @Test
    void GivenNotLoggedInUser_WhenLogoutUser_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setLoggedIn(false);

        assertThrows(RuntimeException.class, () -> handler.logoutUser(user));
    }

    @Test
    void GivenNullUser_WhenLogoutUser_ThenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> handler.logoutUser(null));
    }

    @Test
    void GivenUser_WhenMapToDTO_ThenFieldsMappedCorrectly() {
        UserInfo user = buildMemberUser();
        UserDTO dto = handler.mapToDTO(user);

        assertNotNull(dto);
        assertEquals(USER_ID, dto.getUserId());
        assertEquals(USERNAME, dto.getUsername());
        assertEquals(EMAIL, dto.getEmail());
        assertEquals(UserGroupDiscount.STUDENT, dto.getGroupDiscount());
    }

    @Test
    void GivenTwoCalls_WhenGenerateUniqueId_ThenValuesDiffer() {
        String id1 = handler.generateUniqueId();
        String id2 = handler.generateUniqueId();

        assertNotNull(id1);
        assertNotNull(id2);
        assertFalse(id1.isBlank());
        assertFalse(id2.isBlank());
        assertNotEquals(id1, id2);
    }

    @Test
    void GivenMatchingUserAndToken_WhenValidateUserEditingHisAccount_ThenNoException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);

        assertDoesNotThrow(() ->
            handler.validateUserEditingHisAccount(user, USER_ID, TOKEN)
        );
    }

    @Test
    void GivenNullUser_WhenValidateUserEditingHisAccount_ThenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () ->
            handler.validateUserEditingHisAccount(null, USER_ID, TOKEN)
        );
    }

    @Test
    void GivenNullToken_WhenValidateUserEditingHisAccount_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(null);

        assertThrows(RuntimeException.class, () ->
            handler.validateUserEditingHisAccount(user, USER_ID, TOKEN)
        );
    }

    @Test
    void GivenTokenMismatch_WhenValidateUserEditingHisAccount_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr("token-a");

        assertThrows(RuntimeException.class, () ->
            handler.validateUserEditingHisAccount(user, USER_ID, "token-b")
        );
    }

    @Test
    void GivenUserIdMismatch_WhenValidateUserEditingHisAccount_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);

        assertThrows(RuntimeException.class, () ->
            handler.validateUserEditingHisAccount(user, "other-user", TOKEN)
        );
    }

    @Test
    void GivenValidInput_WhenEditUsername_ThenNameUpdated() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        handler.editUsername(user, USER_ID, USERNAME, "NewName", TOKEN);

        assertEquals("NewName", user.getName());
    }

    @Test
    void GivenValidInput_WhenEditUsername_ThenSetterCalled() {
        UserInfo user = spy(buildMemberUser());
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        handler.editUsername(user, USER_ID, USERNAME, "NewName", TOKEN);

        verify(user).setName("NewName");
    }

    @Test
    void GivenWrongOldUsername_WhenEditUsername_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);

        assertThrows(RuntimeException.class, () ->
            handler.editUsername(user, USER_ID, "wrong", "NewName", TOKEN)
        );
    }

    @Test
    void GivenValidInput_WhenEditPassword_ThenPasswordUpdated() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        handler.editPassword(user, USER_ID, PASSWORD, "new-pass", TOKEN);

        assertTrue(PasswordEncoderUtil.matches("new-pass", user.getPassword()));
        assertFalse(PasswordEncoderUtil.matches(PASSWORD, user.getPassword()));
    }

    @Test
    void GivenValidInput_WhenEditPassword_ThenSetterCalled() {
        UserInfo user = spy(buildMemberUser());
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        handler.editPassword(user, USER_ID, PASSWORD, "new-pass", TOKEN);

        verify(user).setPassword(anyString());
    }

    @Test
    void GivenWrongOldPassword_WhenEditPassword_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);

        assertThrows(RuntimeException.class, () ->
            handler.editPassword(user, USER_ID, "wrong-old", "new-pass", TOKEN)
        );
    }

    @Test
    void GivenEmptyNewPassword_WhenEditPassword_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);

        assertThrows(RuntimeException.class, () ->
            handler.editPassword(user, USER_ID, PASSWORD, "", TOKEN)
        );
    }

    @Test
    void GivenValidInput_WhenEditEmail_ThenEmailUpdated() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        handler.editEmail(user, USER_ID, EMAIL, "new@mail.com", TOKEN);

        assertEquals("new@mail.com", user.getEmail());
    }

    @Test
    void GivenValidInput_WhenEditEmail_ThenSetterCalled() {
        UserInfo user = spy(buildMemberUser());
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);
        handler.editEmail(user, USER_ID, EMAIL, "new@mail.com", TOKEN);

        verify(user).setEmail("new@mail.com");
    }

    @Test
    void GivenInvalidNewEmail_WhenEditEmail_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        assertThrows(RuntimeException.class, () ->
            handler.editEmail(user, USER_ID, EMAIL, "bad-email", TOKEN)
        );
    }

    @Test
    void GivenWrongOldEmail_WhenEditEmail_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        assertThrows(RuntimeException.class, () ->
            handler.editEmail(user, USER_ID, "wrong@mail.com", "new@mail.com", TOKEN)
        );
    }

    @Test
    void GivenLoggedInUser_WhenSetUserGroupDiscount_ThenUpdated() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        handler.setUserGroupDiscount(user, USER_ID, UserGroupDiscount.SENIOR, TOKEN);

        assertEquals(UserGroupDiscount.SENIOR, user.getUserGroupDiscount());
    }

    @Test
    void GivenLoggedInUser_WhenSetUserGroupDiscount_ThenSetterCalled() {
        UserInfo user = spy(buildMemberUser());
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        handler.setUserGroupDiscount(user, USER_ID, UserGroupDiscount.SENIOR, TOKEN);

        verify(user).setUserGroupDiscount(UserGroupDiscount.SENIOR);
    }

    @Test
    void GivenNullDiscount_WhenSetUserGroupDiscount_ThenSetToNone() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(true);

        handler.setUserGroupDiscount(user, USER_ID, null, TOKEN);

        assertEquals(UserGroupDiscount.NONE, user.getUserGroupDiscount());
    }

    @Test
    void GivenNotLoggedInUser_WhenSetUserGroupDiscount_ThenThrowIllegalStateException() {
        UserInfo user = buildMemberUser();
        user.setSessionTokenStr(TOKEN);
        user.setLoggedIn(false);

        assertThrows(RuntimeException.class, () ->
            handler.setUserGroupDiscount(user, USER_ID, UserGroupDiscount.SENIOR, TOKEN)
        );
    }

    @Test
    void GivenGuestUser_WhenAddProductionRole_ThenProductionInitialized() {
        UserInfo guest = buildGuestUser();
        guest.setLoggedIn(true);

        handler.addProductionRole(guest, 10, UserProduction.RoleInProduction.MANAGER);

        assertNotNull(guest.getUserProduction());
        assertEquals(UserProduction.RoleInProduction.MANAGER, guest.getUserProduction().getProductionRole(10));
    }

    @Test
    void GivenNullUser_WhenValidateGuest_ThenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> handler.validateGuest(null));
    }

    @Test
    void GivenNonGuestUser_WhenValidateGuest_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();
        user.setLoggedIn(true);

        assertThrows(RuntimeException.class, () -> handler.validateGuest(user));
    }

    @Test
    void GivenGuestUser_WhenValidateGuest_ThenNoException() {
        UserInfo guest = buildGuestUser();
        guest.setLoggedIn(true);

        assertDoesNotThrow(() -> handler.validateGuest(guest));
    }

    @Test
    void GivenNullUser_WhenValidateUserDoesNotExist_ThenNoException() {
        assertDoesNotThrow(() -> handler.validateUserDoesNotExist(null));
    }

    @Test
    void GivenExistingUser_WhenValidateUserDoesNotExist_ThenThrowRuntimeException() {
        UserInfo user = buildMemberUser();

        assertThrows(RuntimeException.class, () -> handler.validateUserDoesNotExist(user));
    }

    @Test
    void GivenNullUser_WhenValidateUserFound_ThenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> handler.validateUserFound(null));
    }

    @Test
    void GivenExistingUser_WhenValidateUserFound_ThenNoException() {
        UserInfo user = buildMemberUser();

        assertDoesNotThrow(() -> handler.validateUserFound(user));
    }

    @Test
    void GivenMemberUser_WhenIsUserRegistered_ThenReturnTrue() {
        UserInfo user = buildMemberUser();

        assertTrue(handler.isUserRegistered(user));
    }

    @Test
    void GivenGuestOrNullUser_WhenIsUserRegistered_ThenReturnFalse() {
        UserInfo guest = buildGuestUser();

        assertFalse(handler.isUserRegistered(guest));
        assertFalse(handler.isUserRegistered(null));
    }
}
