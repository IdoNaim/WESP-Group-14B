package com.ticketpurchasingsystem.project.init;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Robustness tests for the init subsystem.
 *
 * Requirement: The system must be resilient to failures from external systems
 * and the database during initialization. If any init command fails, the whole
 * initialization must fail and halt with an appropriate error.
 *
 * These tests use mocks to simulate external system failures (DB down,
 * payment service unresponsive, etc.) without hitting real infrastructure.
 * A dedicated test application.properties should be used so real DB is never touched.
 */
@ExtendWith(MockitoExtension.class)
class InitRobustnessTest {

    @Mock private UserService userService;
    @Mock private ProductionService productionService;
    @Mock private EventService eventService;
    @Mock private HistoryOrderService historyOrderService;
    @Mock private ActiveOrderService activeOrderService;

    private InitCommandExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new InitCommandExecutor(
                userService, productionService, eventService,
                historyOrderService, activeOrderService);
    }

    // ── DB / external system failures during init ─────────────────────────────

    @Test
    void GivenDbDownOnFirstCommand_WhenInitExecuted_ThenInitFailsImmediately() {
        when(userService.guestEntry())
                .thenThrow(new RuntimeException("Connection refused: DB unavailable"));

        ParsedCommand cmd = new ParsedCommand("g1", "guest-entry", List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> executor.execute(cmd));
        assertTrue(ex.getMessage().contains("Connection refused") || ex.getCause() != null);
    }

    @Test
    void GivenDbFailsOnThirdCommand_WhenInitExecuted_ThenSubsequentCommandsNotRun() {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Timeout: DB not responding"));

        // Command 1: guest-entry — succeeds
        executor.execute(new ParsedCommand("g1", "guest-entry", List.of()));
        verify(userService).guestEntry();

        // Command 2: register — succeeds
        executor.execute(new ParsedCommand(null, "register",
                List.of("$g1", "alice", "Alice Smith", "pass123", "alice@example.com", "NONE")));

        // Command 3: login — DB throws
        ParsedCommand loginCmd = new ParsedCommand("tok", "login",
                List.of("$g1", "alice", "pass123"));
        assertThrows(RuntimeException.class, () -> executor.execute(loginCmd));

        // Command 4 would have been logout — must never be called
        verify(userService, never()).logoutUser(anyString(), anyString());
    }

    @Test
    void GivenProductionServiceThrowsDuringInit_WhenCreateCompanyExecuted_ThenInitFails() {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString())).thenReturn("alice-tok");
        when(productionService.createProductionCompany(anyString(), any()))
                .thenThrow(new RuntimeException("External payment service unreachable"));

        executor.execute(new ParsedCommand("g1", "guest-entry", List.of()));
        executor.execute(new ParsedCommand(null, "register",
                List.of("$g1", "alice", "Alice", "pass", "a@a.com", "NONE")));
        executor.execute(new ParsedCommand("tok", "login",
                List.of("$g1", "alice", "pass")));

        ParsedCommand createCompany = new ParsedCommand("c1", "create-production-company",
                List.of("$tok", "Live Events Co.", "Desc", "contact@liveevents.com"));

        assertThrows(RuntimeException.class, () -> executor.execute(createCompany));
        // Nothing after this command should have been attempted
        verify(eventService, never()).createEvent(anyString(), any(), any(), any());
    }

    @Test
    void GivenEventServiceThrowsDuringInit_WhenCreateEventExecuted_ThenInitFails() {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString())).thenReturn("alice-tok");
        when(productionService.createProductionCompany(anyString(), any())).thenReturn(1);
        when(eventService.createEvent(anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB constraint violation"));

        executor.execute(new ParsedCommand("g1", "guest-entry", List.of()));
        executor.execute(new ParsedCommand(null, "register",
                List.of("$g1", "alice", "Alice", "pass", "a@a.com", "NONE")));
        executor.execute(new ParsedCommand("tok", "login",
                List.of("$g1", "alice", "pass")));
        executor.execute(new ParsedCommand("c1", "create-production-company",
                List.of("$tok", "Live Events Co.", "Desc", "contact@liveevents.com")));

        ParsedCommand createEvent = new ParsedCommand("e1", "create-event",
                List.of("$tok", "ev1", "$c1", "Rock Night", "500",
                        "2026-07-11T20:00", "true", "Tel Aviv Arena"));

        assertThrows(RuntimeException.class, () -> executor.execute(createEvent));
    }

    @Test
    void GivenActiveOrderServiceThrowsDuringInit_WhenCreateActiveOrderExecuted_ThenInitFails() {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString())).thenReturn("bob-tok");
        when(activeOrderService.createPendingOrder(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DB write failed"));

        executor.execute(new ParsedCommand("g2", "guest-entry", List.of()));
        executor.execute(new ParsedCommand("tok", "login",
                List.of("$g2", "bob", "pass456")));

        ParsedCommand createOrder = new ParsedCommand("o1", "create-active-order",
                List.of("$tok", "bob", "1"));

        assertThrows(RuntimeException.class, () -> executor.execute(createOrder));
    }

    @Test
    void GivenHistoryOrderServiceThrowsDuringInit_WhenCreateHistoryOrderExecuted_ThenInitFails() {
        when(historyOrderService.createHistoryOrder(anyString(), anyString(), anyString(),
                anyInt(), any(), anyDouble(), anyList(), any()))
                .thenThrow(new RuntimeException("DB write failed for history order"));

        // create-history-order(orderId, userId, eventId, companyId, price, seat1, seat2)
        ParsedCommand createHistory = new ParsedCommand(null, "create-history-order",
                List.of("order1", "bob", "ev1", "1", "100.0", "A1", "A2"));

        assertThrows(RuntimeException.class, () -> executor.execute(createHistory));
    }

    // ── Malformed / logically invalid init files ──────────────────────────────

    @Test
    void GivenInitFileWithDuplicateUserRegistration_WhenSecondRegisterFails_ThenInitHalts() {
        when(userService.guestEntry()).thenReturn("gt1");
        // First register succeeds, second throws (duplicate user)
        doNothing()
                .doThrow(new RuntimeException("User 'alice' already exists"))
                .when(userService).registerUser(anyString(), anyString(), anyString(),
                        anyString(), any(), anyString());

        executor.execute(new ParsedCommand("g1", "guest-entry", List.of()));
        executor.execute(new ParsedCommand(null, "register",
                List.of("$g1", "alice", "Alice", "pass", "a@a.com", "NONE")));

        ParsedCommand duplicate = new ParsedCommand(null, "register",
                List.of("$g1", "alice", "Alice Again", "pass2", "a2@a.com", "NONE"));

        assertThrows(RuntimeException.class, () -> executor.execute(duplicate));
    }

    @Test
    void GivenLoginWithWrongPassword_WhenInitExecuted_ThenInitFails() {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        executor.execute(new ParsedCommand("g1", "guest-entry", List.of()));

        ParsedCommand login = new ParsedCommand("tok", "login",
                List.of("$g1", "alice", "WRONG_PASSWORD"));

        assertThrows(RuntimeException.class, () -> executor.execute(login));
    }

    @Test
    void GivenCreateProductionWithNonExistentOwner_WhenAssignOwnerFails_ThenInitFails() {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString())).thenReturn("alice-tok");
        when(productionService.createProductionCompany(anyString(), any())).thenReturn(42);
        doThrow(new RuntimeException("User 'ghost' not found"))
                .when(productionService).assignOwner(anyString(), anyInt(), eq("ghost"));

        executor.execute(new ParsedCommand("g1", "guest-entry", List.of()));
        executor.execute(new ParsedCommand("tok", "login", List.of("$g1", "alice", "pass")));
        executor.execute(new ParsedCommand("c1", "create-production-company",
                List.of("$tok", "Co", "Desc", "c@c.com")));

        ParsedCommand assignOwner = new ParsedCommand(null, "assign-owner",
                List.of("$tok", "$c1", "ghost"));

        assertThrows(RuntimeException.class, () -> executor.execute(assignOwner));
    }

    // ── Full-file robustness via InitFileLoader ────────────────────────────────

    @Test
    void GivenInitFileThatWouldFailMidway_WhenLoaderRuns_ThenNoCommandsAfterFailureAreExecuted() throws Exception {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Simulated DB failure"));

        Path tmp = Files.createTempFile("robustness_test_init", ".txt");
        Files.writeString(tmp,
                "$g1 = guest-entry();\n" +
                        "register($g1, alice, Alice, pass, a@a.com, NONE);\n" +
                        // login will throw:
                        "$tok = login($g1, alice, pass);\n" +
                        // These must NEVER execute:
                        "$c1 = create-production-company($tok, Co, Desc, c@c.com);\n" +
                        "$e1 = create-event($tok, ev1, $c1, Rock Night, 100, 2026-07-01T20:00, false, TLV);\n");

        InitFileLoader loader = buildLoader(tmp.toString());

        // A failing command must abort the run by throwing (the loader no longer calls System.exit).
        assertThrows(RuntimeException.class,
                () -> loader.run(new DefaultApplicationArguments()));

        verify(productionService, never()).createProductionCompany(anyString(), any());
        verify(eventService, never()).createEvent(anyString(), any(), any(), any());
    }

    @Test
    void GivenInitFileFailsMidway_WhenLoaderRuns_ThenThrownErrorIdentifiesFailingCommand() throws Exception {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        Path tmp = Files.createTempFile("error_context_init", ".txt");
        Files.writeString(tmp,
                "$g1 = guest-entry();\n" +
                        "$tok = login($g1, alice, pass);\n");

        InitFileLoader loader = buildLoader(tmp.toString());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.run(new DefaultApplicationArguments()));

        // The loader should wrap the failure with which command (by name and index) broke init.
        assertTrue(ex.getMessage().contains("login"),
                "Expected the failing command name in the error, but was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Init failed at command"),
                "Expected init-failure context in the error, but was: " + ex.getMessage());
        // The original cause must be preserved for diagnosis.
        assertNotNull(ex.getCause());
    }

    @Test
    void GivenInitFileWithOnlyCommentsAndSpaces_WhenLoaderRuns_ThenNoServicesCalledAndNoError() throws Exception {
        Path tmp = Files.createTempFile("comment_only_init", ".txt");
        Files.writeString(tmp, "# just a comment\n\n# another one\n");

        InitFileLoader loader = buildLoader(tmp.toString());
        loader.run(new org.springframework.boot.DefaultApplicationArguments());

        verifyNoInteractions(userService, productionService, eventService,
                historyOrderService, activeOrderService);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InitFileLoader buildLoader(String initFilePath) throws Exception {
        InitFileLoader loader = new InitFileLoader(
                userService, productionService, eventService,
                historyOrderService, activeOrderService);

        var field = InitFileLoader.class.getDeclaredField("initFilePath");
        field.setAccessible(true);
        field.set(loader, initFilePath);

        return loader;
    }
}