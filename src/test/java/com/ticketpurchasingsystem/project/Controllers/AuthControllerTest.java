package com.ticketpurchasingsystem.project.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.Controllers.apidto.LoginRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.ProfileUpdateRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.RegisterRequestDTO;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.User.UserState;
import com.ticketpurchasingsystem.project.domain.User.UserProduction;
import com.ticketpurchasingsystem.project.domain.systemAdmin.IAdminRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private IUserService userService;

        @MockBean
        private AuthenticationService authenticationService;

        @MockBean
        private IAdminRepo adminRepo;

        private static final String VALID_AUTH = "Bearer valid-token";

        // guest entry
        // POST /api/identity/guest

        @Test
        void WhenGuestEntry_ThenReturn200WithToken() throws Exception {
                when(userService.guestEntry()).thenReturn("guest-token-123");

                mockMvc.perform(post("/api/identity/guest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("guest-token-123"));
        }

        @Test
        void WhenGuestEntryFails_ThenReturn500WithError() throws Exception {
                when(userService.guestEntry()).thenThrow(new RuntimeException("Session store unavailable"));

                mockMvc.perform(post("/api/identity/guest"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error").exists());
        }

        // register
        // POST /api/identity/register

        @Test
        void GivenValidRequest_WhenRegister_ThenReturn201WithMessage() throws Exception {
                RegisterRequestDTO dto = new RegisterRequestDTO();
                dto.setUserId("eden");
                dto.setName("Eden Yaakobi");
                dto.setPassword("pass123");
                dto.setEmail("eden@test.com");
                dto.setUserGroupDiscount(UserGroupDiscount.NONE);
                doNothing().when(userService).registerUser(any(), any(), any(), any(), any(), any());

                mockMvc.perform(post("/api/identity/register")
                                .header("Authorization", VALID_AUTH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("User registered successfully."));
        }

        @Test
        void GivenDuplicateUserId_WhenRegister_ThenReturn400WithError() throws Exception {
                RegisterRequestDTO dto = new RegisterRequestDTO();
                dto.setUserId("eden");
                dto.setName("Eden");
                dto.setPassword("pass");
                dto.setEmail("eden@test.com");
                doThrow(new RuntimeException("User already exists")).when(userService)
                                .registerUser(any(), any(), any(), any(), any(), any());

                mockMvc.perform(post("/api/identity/register")
                                .header("Authorization", VALID_AUTH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
        }

        // login
        // POST /api/identity/login

        @Test
        void GivenValidCredentials_WhenLogin_ThenReturn200WithTokenAndUserId() throws Exception {
                LoginRequestDTO dto = new LoginRequestDTO();
                dto.setUserId("eden");
                dto.setPassword("pass123");
                when(userService.loginUser(eq("eden"), eq("pass123"), any())).thenReturn("session-token-xyz");

                mockMvc.perform(post("/api/identity/login")
                                .header("Authorization", VALID_AUTH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("session-token-xyz"))
                                .andExpect(jsonPath("$.userId").value("eden"));
        }

        @Test
        void GivenWrongPassword_WhenLogin_ThenReturn401WithError() throws Exception {
                LoginRequestDTO dto = new LoginRequestDTO();
                dto.setUserId("eden");
                dto.setPassword("wrong");
                doThrow(new RuntimeException("Invalid credentials")).when(userService)
                                .loginUser(any(), any(), any());

                mockMvc.perform(post("/api/identity/login")
                                .header("Authorization", VALID_AUTH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").exists());
        }

        // logout
        // POST /api/identity/logout

        // logout should return the session token for the new guest that established after logout for the users
        // fetch the session token and validate that session token is retrived
        @Test
        void GivenLoggedInUser_WhenLogout_ThenReturn200WithMessage() throws Exception {
                 // logout should return the session token for the new guest that established after logout for the users
                // fetch the session token and validate that session token is retrived
                // do nothing is not good here because logout is returning a new guest session token and do nothing is for void 
                when(userService.logoutUser(any(), any())).thenReturn("new-guest-token-456");
                // validate that the new guest token is retrived and valid along with the 200 response


                mockMvc.perform(post("/api/identity/logout")
                                .header("Authorization", VALID_AUTH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("userId", "eden"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logged out successfully."))
                                .andExpect(jsonPath("$.token").value("new-guest-token-456"));
        }

        @Test
        void GivenInvalidToken_WhenLogout_ThenReturn400WithError() throws Exception {
                doThrow(new RuntimeException("Invalid session")).when(userService).logoutUser(any(), any());

                mockMvc.perform(post("/api/identity/logout")
                                .header("Authorization", "Bearer bad-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("userId", "eden"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
        }

        // exit
        // POST /api/identity/exit

        @Test
        void GivenValidToken_WhenExit_ThenReturn200WithMessage() throws Exception {
                doNothing().when(userService).Exit(any());

                mockMvc.perform(post("/api/identity/exit")
                                .header("Authorization", VALID_AUTH))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Exited platform successfully."));
        }

        @Test
        void GivenInvalidToken_WhenExit_ThenReturn400WithError() throws Exception {
                doThrow(new RuntimeException("Token not found")).when(userService).Exit(any());

                mockMvc.perform(post("/api/identity/exit")
                                .header("Authorization", "Bearer bad-token"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
        }

        // --- Identity Management Tasks 4-6 ---

        // GET /api/identity/me

        @Test
        void GivenValidToken_WhenGetCurrentUser_ThenReturnsUserDetailsExcludingPassword() throws Exception {
                UserDTO userDTO = new UserDTO("test-user-id", "john_doe", "john@example.com", UserGroupDiscount.NONE);

                when(authenticationService.validate("valid-token")).thenReturn(true);
                when(authenticationService.getUser("valid-token")).thenReturn("test-user-id");
                when(userService.getUser("test-user-id")).thenReturn(userDTO);

                mockMvc.perform(get("/api/identity/me")
                                .header("Authorization", "Bearer valid-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value("test-user-id"))
                                .andExpect(jsonPath("$.name").value("john_doe"))
                                .andExpect(jsonPath("$.email").value("john@example.com"))
                                .andExpect(jsonPath("$.userGroupDiscount").value("NONE"))
                                .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        void GivenInvalidToken_WhenGetCurrentUser_ThenReturnsUnauthorized() throws Exception {
                when(authenticationService.validate("invalid-token")).thenReturn(false);

                mockMvc.perform(get("/api/identity/me")
                                .header("Authorization", "Bearer invalid-token"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        void GivenMissingBearerPrefix_WhenGetCurrentUser_ThenStillExtractsTokenAndProcesses() throws Exception {
                UserDTO userDTO = new UserDTO("test-user-id", "john_doe", "john@example.com", UserGroupDiscount.NONE);

                when(authenticationService.validate("valid-token")).thenReturn(true);
                when(authenticationService.getUser("valid-token")).thenReturn("test-user-id");
                when(userService.getUser("test-user-id")).thenReturn(userDTO);

                mockMvc.perform(get("/api/identity/me")
                                .header("Authorization", "valid-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value("test-user-id"));
        }

        // PUT /api/identity/profile

        @Test
        void GivenValidTokenAndNewProfileDetails_WhenUpdateProfile_ThenUpdatesSuccessfully() throws Exception {
                ProfileUpdateRequestDTO requestDTO = new ProfileUpdateRequestDTO();
                requestDTO.setName("new_name");
                requestDTO.setEmail("new@example.com");
                requestDTO.setUserGroupDiscount(UserGroupDiscount.STUDENT);

                UserDTO userDTO = new UserDTO("test-user-id", "john_doe", "john@example.com", UserGroupDiscount.NONE);

                when(authenticationService.validate("valid-token")).thenReturn(true);
                when(authenticationService.getUser("valid-token")).thenReturn("test-user-id");
                when(userService.getUser("test-user-id")).thenReturn(userDTO);

                mockMvc.perform(put("/api/identity/profile")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Profile updated successfully."));

                verify(userService).editUsername("test-user-id", "john_doe", "new_name", "valid-token");
                verify(userService).editEmail("test-user-id", "john@example.com", "new@example.com", "valid-token");
                verify(userService).setUserGroupDiscount("test-user-id", UserGroupDiscount.STUDENT, "valid-token");
        }

        @Test
        void GivenValidTokenAndNoChanges_WhenUpdateProfile_ThenNoUpdatesExecuted() throws Exception {
                ProfileUpdateRequestDTO requestDTO = new ProfileUpdateRequestDTO();
                requestDTO.setName("john_doe");
                requestDTO.setEmail("john@example.com");
                requestDTO.setUserGroupDiscount(UserGroupDiscount.NONE);

                UserDTO userDTO = new UserDTO("test-user-id", "john_doe", "john@example.com", UserGroupDiscount.NONE);

                when(authenticationService.validate("valid-token")).thenReturn(true);
                when(authenticationService.getUser("valid-token")).thenReturn("test-user-id");
                when(userService.getUser("test-user-id")).thenReturn(userDTO);

                mockMvc.perform(put("/api/identity/profile")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Profile updated successfully."));

                verify(userService, never()).editUsername(any(), any(), any(), any());
                verify(userService, never()).editEmail(any(), any(), any(), any());
                verify(userService, never()).setUserGroupDiscount(any(), any(), any());
        }

        @Test
        void GivenInvalidToken_WhenUpdateProfile_ThenReturnsUnauthorized() throws Exception {
                ProfileUpdateRequestDTO requestDTO = new ProfileUpdateRequestDTO();

                when(authenticationService.validate("invalid-token")).thenReturn(false);

                mockMvc.perform(put("/api/identity/profile")
                                .header("Authorization", "Bearer invalid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").exists());
        }

        // GET /api/identity/permissions

        @Test
        void GivenValidToken_WhenGetPermissions_ThenReturnsUserPermissionsAndState() throws Exception {
                UserInfo userInfo = new UserInfo("test-user-id", "john_doe", "john@example.com", "pass", UserGroupDiscount.NONE);
                userInfo.setUserState(UserState.MEMBER);
                
                UserProduction userProd = new UserProduction();
                userProd.addProduction(101, UserProduction.RoleInProduction.FOUNDER);
                userInfo.setUserProduction(userProd);

                when(authenticationService.validate("valid-token")).thenReturn(true);
                when(authenticationService.getUser("valid-token")).thenReturn("test-user-id");
                when(userService.getUserInfo("test-user-id")).thenReturn(userInfo);
                when(adminRepo.isAdmin("test-user-id")).thenReturn(true);

                mockMvc.perform(get("/api/identity/permissions")
                                .header("Authorization", "Bearer valid-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value("test-user-id"))
                                .andExpect(jsonPath("$.state").value("MEMBER"))
                                .andExpect(jsonPath("$.isAdmin").value(true))
                                .andExpect(jsonPath("$.productionRoles['101']").value("FOUNDER"));
        }

        @Test
        void GivenInvalidToken_WhenGetPermissions_ThenReturnsUnauthorized() throws Exception {
                when(authenticationService.validate("invalid-token")).thenReturn(false);

                mockMvc.perform(get("/api/identity/permissions")
                                .header("Authorization", "Bearer invalid-token"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").exists());
        }
}
