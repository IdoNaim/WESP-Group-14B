package com.ticketpurchasingsystem.project.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.Controllers.apidto.LoginRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.RegisterRequestDTO;
import com.ticketpurchasingsystem.project.application.UserService.IUserService;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
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

        @Test
        void GivenLoggedInUser_WhenLogout_ThenReturn200WithMessage() throws Exception {
                doNothing().when(userService).logoutUser(any(), any());

                mockMvc.perform(post("/api/identity/logout")
                                .header("Authorization", VALID_AUTH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("userId", "eden"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Logged out successfully."));
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
}
