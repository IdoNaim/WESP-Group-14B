package com.ticketpurchasingsystem.project.acceptance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.Controllers.AuthController;
import com.ticketpurchasingsystem.project.Controllers.apidto.LoginRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.RegisterRequestDTO;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.SystemAdminService;
import com.ticketpurchasingsystem.project.application.UserService.UserPublisher;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserHandler;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.MemoryUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance tests for the AuthController HTTP layer.
 * Uses real services wired without Spring context — same pattern as existing
 * acceptance tests
 * but verified through HTTP requests instead of direct service calls.
 * ApplicationEventPublisher is mocked (no-op) since it is Spring
 * infrastructure, not business logic.
 */
class AuthApiAcceptanceTest {

        private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private UserService userService;
        private AuthenticationService authService;
        


        @BeforeEach
        void setUp() {
                InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
                DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);

                ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
                domainAuthService.init();
                authService = new AuthenticationService(domainAuthService, sessionRepo);

                MemoryUserRepo userRepo = new MemoryUserRepo();
                UserHandler userHandler = new UserHandler();
                ApplicationEventPublisher noopPublisher = mock();
                UserPublisher userPublisher = new UserPublisher(noopPublisher);
                userService = new UserService(userRepo, userHandler, authService, userPublisher);

                mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService)).build();
                objectMapper = new ObjectMapper();
        }

        // guest entry
        // POST /api/identity/guest

        @Test
        void WhenGuestEntry_ThenReturn200WithToken() throws Exception {
                mockMvc.perform(post("/api/identity/guest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").exists());
        }

        @Test
        void WhenTwoGuestEntries_ThenEachGetsDistinctToken() throws Exception {
                MvcResult first = mockMvc.perform(post("/api/identity/guest"))
                                .andExpect(status().isOk())
                                .andReturn();
                MvcResult second = mockMvc.perform(post("/api/identity/guest"))
                                .andExpect(status().isOk())
                                .andReturn();

                String token1 = objectMapper.readValue(first.getResponse().getContentAsString(), Map.class)
                                .get("token").toString();
                String token2 = objectMapper.readValue(second.getResponse().getContentAsString(), Map.class)
                                .get("token").toString();

                assert !token1.equals(token2) : "Two guest sessions must have distinct tokens";
        }

        // register user
        // POST /api/identity/register

        @Test
        void GivenGuestToken_WhenRegisterUser_ThenReturn201WithMessage() throws Exception {
                String guestToken = extractToken(mockMvc.perform(post("/api/identity/guest"))
                                .andReturn());

                RegisterRequestDTO dto = new RegisterRequestDTO();
                dto.setUserId("eden");
                dto.setName("Eden Yaakobi");
                dto.setPassword("pass123");
                dto.setEmail("eden@test.com");
                dto.setUserGroupDiscount(UserGroupDiscount.NONE);

                mockMvc.perform(post("/api/identity/register")
                                .header("Authorization", "Bearer " + guestToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("User registered successfully."));
        }

        @Test
        void GivenDuplicateRegistration_WhenRegisterUser_ThenReturn400WithError() throws Exception {
                String guestToken = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());
                RegisterRequestDTO dto = new RegisterRequestDTO();
                dto.setUserId("duplicate-user");
                dto.setName("Dup");
                dto.setPassword("pass");
                dto.setEmail("dup@test.com");
                dto.setUserGroupDiscount(UserGroupDiscount.NONE);

                // First registration succeeds
                mockMvc.perform(post("/api/identity/register")
                                .header("Authorization", "Bearer " + guestToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated());

                // Second registration with same userId fails
                String guestToken2 = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());
                mockMvc.perform(post("/api/identity/register")
                                .header("Authorization", "Bearer " + guestToken2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
        }

        // login
        // POST /api/identity/login

        @Test
        void GivenRegisteredUser_WhenLogin_ThenReturn200WithToken() throws Exception {
                // Register first
                String guestToken = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());
                RegisterRequestDTO reg = new RegisterRequestDTO();
                reg.setUserId("loginUser");
                reg.setName("Login User");
                reg.setPassword("mypassword");
                reg.setEmail("login@test.com");
                reg.setUserGroupDiscount(UserGroupDiscount.NONE);
                mockMvc.perform(post("/api/identity/register")
                                .header("Authorization", "Bearer " + guestToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reg)));

                // login
                String loginGuestToken = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());
                LoginRequestDTO login = new LoginRequestDTO();
                login.setUserId("loginUser");
                login.setPassword("mypassword");

                mockMvc.perform(post("/api/identity/login")
                                .header("Authorization", "Bearer " + loginGuestToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").exists())
                                .andExpect(jsonPath("$.userId").value("loginUser"));
        }

        @Test
        void GivenWrongPassword_WhenLogin_ThenReturn401WithError() throws Exception {
                // Register first
                String guestToken = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());
                RegisterRequestDTO reg = new RegisterRequestDTO();
                reg.setUserId("loginUser2");
                reg.setName("Login User 2");
                reg.setPassword("correct-pass");
                reg.setEmail("login2@test.com");
                reg.setUserGroupDiscount(UserGroupDiscount.NONE);
                mockMvc.perform(post("/api/identity/register")
                                .header("Authorization", "Bearer " + guestToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(reg)));

                // Login with wrong password
                String loginGuestToken = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());
                LoginRequestDTO login = new LoginRequestDTO();
                login.setUserId("loginUser2");
                login.setPassword("wrong-pass");

                mockMvc.perform(post("/api/identity/login")
                                .header("Authorization", "Bearer " + loginGuestToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(login)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").exists());
        }

        // logout
        // POST /api/identity/logout

        @Test
        void GivenLoggedInUser_WhenLogout_ThenReturn200WithMessage() throws Exception {
                // Register and login via UserService so the user exists in the repo
                String guestToken = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());
                RegisterRequestDTO reg = new RegisterRequestDTO();
                reg.setUserId("logout-user");
                reg.setName("Logout User");
                reg.setPassword("pass");
                reg.setEmail("logout@test.com");
                reg.setUserGroupDiscount(UserGroupDiscount.NONE);
                mockMvc.perform(post("/api/identity/register")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)));

                String loginGuestToken = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());
                LoginRequestDTO login = new LoginRequestDTO();
                login.setUserId("logout-user");
                login.setPassword("pass");
                String userToken = extractToken(mockMvc.perform(post("/api/identity/login")
                        .header("Authorization", "Bearer " + loginGuestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login))).andReturn());

                mockMvc.perform(post("/api/identity/logout")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("userId", "logout-user"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logged out successfully."));
        }

        // exit
        // POST /api/identity/exit

        @Test
        void GivenGuestToken_WhenExit_ThenReturn200WithMessage() throws Exception {
                String guestToken = extractToken(mockMvc.perform(post("/api/identity/guest")).andReturn());

                mockMvc.perform(post("/api/identity/exit")
                                .header("Authorization", "Bearer " + guestToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Exited platform successfully."));
        }

        // helper method

        @SuppressWarnings("unchecked")
        private String extractToken(MvcResult result) throws Exception {
                return objectMapper
                                .readValue(result.getResponse().getContentAsString(), Map.class)
                                .get("token").toString();
        }
}
