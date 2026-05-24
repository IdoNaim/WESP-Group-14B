package com.ticketpurchasingsystem.project.Controllers;

import com.ticketpurchasingsystem.project.Controllers.apidto.ProfileUpdateRequestDTO;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.User.UserState;
import com.ticketpurchasingsystem.project.domain.User.UserProduction;
import com.ticketpurchasingsystem.project.domain.systemAdmin.IAdminRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private IUserService userService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private IAdminRepo adminRepo;

    @InjectMocks
    private AuthController authController;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String USER_ID = "test-user-id";

    @Test
    void GivenValidToken_WhenGetCurrentUser_ThenReturnsUserDetailsExcludingPassword() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        UserDTO userDTO = new UserDTO(USER_ID, "john_doe", "john@example.com", UserGroupDiscount.NONE);

        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.getUser(USER_ID)).thenReturn(userDTO);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getCurrentUser(authHeader);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(USER_ID, body.get("userId"));
        assertEquals("john_doe", body.get("name"));
        assertEquals("john@example.com", body.get("email"));
        assertEquals(UserGroupDiscount.NONE, body.get("userGroupDiscount"));
        assertFalse(body.containsKey("password")); // CRITICAL: password must not be returned
    }

    @Test
    void GivenInvalidToken_WhenGetCurrentUser_ThenReturnsUnauthorized() {
        // Arrange
        String authHeader = "Bearer " + INVALID_TOKEN;
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getCurrentUser(authHeader);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("error"));
    }

    @Test
    void GivenMissingBearerPrefix_WhenGetCurrentUser_ThenStillExtractsTokenAndProcesses() {
        // Arrange
        String authHeader = VALID_TOKEN;
        UserDTO userDTO = new UserDTO(USER_ID, "john_doe", "john@example.com", UserGroupDiscount.NONE);

        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.getUser(USER_ID)).thenReturn(userDTO);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getCurrentUser(authHeader);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(USER_ID, response.getBody().get("userId"));
    }

    @Test
    void GivenValidTokenAndNewProfileDetails_WhenUpdateProfile_ThenUpdatesSuccessfully() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        ProfileUpdateRequestDTO requestDTO = new ProfileUpdateRequestDTO();
        requestDTO.setName("new_name");
        requestDTO.setEmail("new@example.com");
        requestDTO.setUserGroupDiscount(UserGroupDiscount.STUDENT);

        UserDTO userDTO = new UserDTO(USER_ID, "john_doe", "john@example.com", UserGroupDiscount.NONE);

        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.getUser(USER_ID)).thenReturn(userDTO);

        // Act
        ResponseEntity<Map<String, String>> response = authController.updateProfile(authHeader, requestDTO);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Profile updated successfully.", response.getBody().get("message"));

        verify(userService).editUsername(USER_ID, "john_doe", "new_name", VALID_TOKEN);
        verify(userService).editEmail(USER_ID, "john@example.com", "new@example.com", VALID_TOKEN);
        verify(userService).setUserGroupDiscount(USER_ID, UserGroupDiscount.STUDENT, VALID_TOKEN);
    }

    @Test
    void GivenValidTokenAndNoChanges_WhenUpdateProfile_ThenNoUpdatesExecuted() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        ProfileUpdateRequestDTO requestDTO = new ProfileUpdateRequestDTO();
        requestDTO.setName("john_doe");
        requestDTO.setEmail("john@example.com");
        requestDTO.setUserGroupDiscount(UserGroupDiscount.NONE);

        UserDTO userDTO = new UserDTO(USER_ID, "john_doe", "john@example.com", UserGroupDiscount.NONE);

        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.getUser(USER_ID)).thenReturn(userDTO);

        // Act
        ResponseEntity<Map<String, String>> response = authController.updateProfile(authHeader, requestDTO);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, never()).editUsername(any(), any(), any(), any());
        verify(userService, never()).editEmail(any(), any(), any(), any());
        verify(userService, never()).setUserGroupDiscount(any(), any(), any());
    }

    @Test
    void GivenInvalidToken_WhenUpdateProfile_ThenReturnsUnauthorized() {
        // Arrange
        String authHeader = "Bearer " + INVALID_TOKEN;
        ProfileUpdateRequestDTO requestDTO = new ProfileUpdateRequestDTO();

        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, String>> response = authController.updateProfile(authHeader, requestDTO);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    void GivenValidToken_WhenGetPermissions_ThenReturnsUserPermissionsAndState() {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        UserInfo userInfo = new UserInfo(USER_ID, "john_doe", "john@example.com", "pass", UserGroupDiscount.NONE);
        userInfo.setUserState(UserState.MEMBER);
        
        // Add a mock production role
        UserProduction userProd = new UserProduction();
        userProd.addProduction(101, UserProduction.RoleInProduction.FOUNDER);
        userInfo.setUserProduction(userProd);

        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(userService.getUserInfo(USER_ID)).thenReturn(userInfo);
        when(adminRepo.isAdmin(USER_ID)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getPermissions(authHeader);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(USER_ID, body.get("userId"));
        assertEquals("MEMBER", body.get("state"));
        assertEquals(true, body.get("isAdmin"));

        Map<Integer, String> productionRoles = (Map<Integer, String>) body.get("productionRoles");
        assertNotNull(productionRoles);
        assertEquals("FOUNDER", productionRoles.get(101));
    }

    @Test
    void GivenInvalidToken_WhenGetPermissions_ThenReturnsUnauthorized() {
        // Arrange
        String authHeader = "Bearer " + INVALID_TOKEN;
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = authController.getPermissions(authHeader);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().containsKey("error"));
    }
}
