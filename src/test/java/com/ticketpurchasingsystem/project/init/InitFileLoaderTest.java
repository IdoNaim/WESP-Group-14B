package com.ticketpurchasingsystem.project.init;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for system initialization from a state file.
 *
 * Requirement: The system must not start if initialization is invalid.
 * Requirement: Empty init file is valid (at least one admin still exists).
 * Requirement: All use-case calls in the init file are legal application-layer calls.
 */
@ExtendWith(MockitoExtension.class)
class InitFileLoaderTest {

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

    // ── Parser tests ──────────────────────────────────────────────────────────

    @Test
    void GivenEmptyFile_WhenParsed_ThenReturnsEmptyCommandList() throws Exception {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream("");

        var commands = parser.parse(input);

        assertTrue(commands.isEmpty());
    }

    @Test
    void GivenOnlyCommentsAndBlankLines_WhenParsed_ThenReturnsEmptyCommandList() throws Exception {
        InitCommandParser parser = new InitCommandParser();
        String content = "# This is a comment\n\n# Another comment\n   \n";
        InputStream input = toStream(content);

        var commands = parser.parse(input);

        assertTrue(commands.isEmpty());
    }

    @Test
    void GivenCommandWithVariableAssignment_WhenParsed_ThenVarNameAndCommandExtracted() throws Exception {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream("$tok = guest-entry();");

        var commands = parser.parse(input);

        assertEquals(1, commands.size());
        ParsedCommand cmd = commands.get(0);
        assertEquals("tok", cmd.varName());
        assertEquals("guest-entry", cmd.name());
        assertTrue(cmd.args().isEmpty());
        assertTrue(cmd.hasVar());
    }

    @Test
    void GivenCommandWithoutVariableAssignment_WhenParsed_ThenVarNameIsNull() throws Exception {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream("logout(alice, tok123);");

        var commands = parser.parse(input);

        assertEquals(1, commands.size());
        assertFalse(commands.get(0).hasVar());
        assertNull(commands.get(0).varName());
    }

    @Test
    void GivenCommandWithMultipleArgs_WhenParsed_ThenAllArgsTrimmedAndExtracted() throws Exception {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream("register($g, alice, Alice Smith, pass123, alice@example.com, NONE);");

        var commands = parser.parse(input);

        var args = commands.get(0).args();
        assertEquals(6, args.size());
        assertEquals("$g", args.get(0));
        assertEquals("alice", args.get(1));
        assertEquals("Alice Smith", args.get(2));
        assertEquals("pass123", args.get(3));
        assertEquals("alice@example.com", args.get(4));
        assertEquals("NONE", args.get(5));
    }

    @Test
    void GivenLineMissingParentheses_WhenParsed_ThenThrowsRuntimeException() {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream("guest-entry;");

        assertThrows(RuntimeException.class, () -> parser.parse(input));
    }

    @Test
    void GivenAssignmentLineMissingEquals_WhenParsed_ThenThrowsRuntimeException() {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream("$tok guest-entry();");

        assertThrows(RuntimeException.class, () -> parser.parse(input));
    }

    // ── Executor tests ────────────────────────────────────────────────────────

    @Test
    void GivenGuestEntryCommand_WhenExecuted_ThenCallsUserServiceAndStoresVariable() {
        when(userService.guestEntry()).thenReturn("guest-token-1");

        ParsedCommand cmd = new ParsedCommand("g1", "guest-entry", java.util.List.of());
        executor.execute(cmd);

        verify(userService, times(1)).guestEntry();
    }

    @Test
    void GivenUndefinedVariable_WhenExecuted_ThenThrowsRuntimeException() {
        // $unknownVar is never assigned
        ParsedCommand cmd = new ParsedCommand(null, "register",
                java.util.List.of("$unknownVar", "alice", "Alice", "pass", "a@a.com", "NONE"));

        assertThrows(RuntimeException.class, () -> executor.execute(cmd));
    }

    @Test
    void GivenUnknownCommandName_WhenExecuted_ThenThrowsRuntimeException() {
        ParsedCommand cmd = new ParsedCommand(null, "nonexistent-command", java.util.List.of());

        assertThrows(RuntimeException.class, () -> executor.execute(cmd));
    }

    @Test
    void GivenMissingRequiredArgument_WhenExecuted_ThenThrowsRuntimeException() {
        // register requires 6 args; give only 2
        ParsedCommand cmd = new ParsedCommand(null, "register",
                java.util.List.of("guestToken", "alice"));

        assertThrows(RuntimeException.class, () -> executor.execute(cmd));
    }

    @Test
    void GivenRegisterCommand_WhenExecuted_ThenDelegatesCorrectlyToUserService() {
        when(userService.guestEntry()).thenReturn("gt1");

        // First get a guest token into context
        executor.execute(new ParsedCommand("g1", "guest-entry", java.util.List.of()));

        // Now register using $g1
        executor.execute(new ParsedCommand(null, "register",
                java.util.List.of("$g1", "alice", "Alice Smith", "pass123", "alice@example.com", "NONE")));

        verify(userService).registerUser(
                eq("alice"), eq("Alice Smith"), eq("pass123"),
                eq("alice@example.com"), any(), eq("gt1"));
    }

    @Test
    void GivenVariableFromPreviousCommand_WhenUsedInNextCommand_ThenResolvedCorrectly() {
        when(userService.guestEntry()).thenReturn("resolved-token");
        when(userService.loginUser(anyString(), anyString(), anyString())).thenReturn("user-session");

        executor.execute(new ParsedCommand("g1", "guest-entry", java.util.List.of()));
        // login($g1, alice, pass123)
        executor.execute(new ParsedCommand("tok", "login",
                java.util.List.of("$g1", "alice", "pass123")));

        verify(userService).loginUser("alice", "pass123", "resolved-token");
    }

    @Test
    void GivenCommandThrowsException_WhenExecuted_ThenExceptionPropagates() {
        when(userService.guestEntry()).thenThrow(new RuntimeException("DB unavailable"));

        ParsedCommand cmd = new ParsedCommand("g1", "guest-entry", java.util.List.of());

        assertThrows(RuntimeException.class, () -> executor.execute(cmd));
    }

    // ── InitFileLoader integration (with temp files) ──────────────────────────

    @Test
    void GivenEmptyInitFile_WhenLoaderRuns_ThenNoCommandsExecutedAndNoException() throws Exception {
        Path tmp = Files.createTempFile("empty_init", ".txt");
        Files.writeString(tmp, "");

        InitFileLoader loader = buildLoader(tmp.toString());

        assertDoesNotThrow(() -> loader.run(new DefaultApplicationArguments()));

        verifyNoInteractions(userService, productionService, eventService,
                historyOrderService, activeOrderService);
    }

    @Test
    void GivenInitFileWithInvalidCommand_WhenLoaderRuns_ThenThrowsInitializationException() throws Exception {
        Path tmp = Files.createTempFile("bad_init", ".txt");
        Files.writeString(tmp, "totally-invalid-command();\n");

        InitFileLoader loader = buildLoader(tmp.toString());

        assertThrows(RuntimeException.class,
                () -> loader.run(new DefaultApplicationArguments()));
    }

    @Test
    void GivenInitFileNotFound_WhenLoaderRuns_ThenSkipsInitializationGracefully() throws Exception {
        InitFileLoader loader = buildLoader("nonexistent/path/init.txt");

        // Should log a warning and return, not throw
        assertDoesNotThrow(() -> loader.run(new DefaultApplicationArguments()));
        verifyNoInteractions(userService);
    }

    @Test
    void GivenValidInitFile_WhenLoaderRuns_ThenAllCommandsExecutedInOrder() throws Exception {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString())).thenReturn("alice-tok");

        Path tmp = Files.createTempFile("valid_init", ".txt");
        Files.writeString(tmp,
                "$g1 = guest-entry();\n" +
                        "register($g1, alice, Alice Smith, pass123, alice@example.com, NONE);\n" +
                        "$alice = login($g1, alice, pass123);\n" +
                        "logout(alice, $alice);\n");

        InitFileLoader loader = buildLoader(tmp.toString());
        loader.run(new DefaultApplicationArguments());

        var inOrder = inOrder(userService);
        inOrder.verify(userService).guestEntry();
        inOrder.verify(userService).registerUser(anyString(), anyString(), anyString(), anyString(), any(), anyString());
        inOrder.verify(userService).loginUser(anyString(), anyString(), anyString());
        inOrder.verify(userService).logoutUser(anyString(), anyString());
    }

    @Test
    void GivenInitFileWhereMiddleCommandFails_WhenLoaderRuns_ThenThrowsAndSubsequentCommandsSkipped()
            throws Exception {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Wrong password"));

        Path tmp = Files.createTempFile("mid_fail_init", ".txt");
        Files.writeString(tmp,
                "$g1 = guest-entry();\n" +
                        "$tok = login($g1, alice, wrongpass);\n" +
                        "logout(alice, $tok);\n");

        InitFileLoader loader = buildLoader(tmp.toString());

        assertThrows(RuntimeException.class,
                () -> loader.run(new DefaultApplicationArguments()));

        verify(userService, never()).logoutUser(anyString(), anyString());
    }

    // ── Parser tests: multi-line / formatting edge cases ──────────────────────

    @Test
    void GivenMultipleCommandLines_WhenParsed_ThenAllReturnedInOrder() throws Exception {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream(
                "$g = guest-entry();\n" +
                        "register($g, alice, Alice, pass, a@a.com, NONE);\n" +
                        "logout(alice, $g);\n");

        var commands = parser.parse(input);

        assertEquals(3, commands.size());
        assertEquals("guest-entry", commands.get(0).name());
        assertEquals("register", commands.get(1).name());
        assertEquals("logout", commands.get(2).name());
    }

    @Test
    void GivenCommandWithoutTrailingSemicolon_WhenParsed_ThenParsedCorrectly() throws Exception {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream("$tok = guest-entry()");

        var commands = parser.parse(input);

        assertEquals(1, commands.size());
        assertEquals("tok", commands.get(0).varName());
        assertEquals("guest-entry", commands.get(0).name());
    }

    @Test
    void GivenAssignmentWithSurroundingWhitespace_WhenParsed_ThenVarAndCommandTrimmed() throws Exception {
        InitCommandParser parser = new InitCommandParser();
        InputStream input = toStream("   $tok    =    guest-entry()  ;  ");

        var commands = parser.parse(input);

        assertEquals(1, commands.size());
        assertEquals("tok", commands.get(0).varName());
        assertEquals("guest-entry", commands.get(0).name());
        assertTrue(commands.get(0).args().isEmpty());
    }

    @Test
    void GivenMalformedCommandOnLaterLine_WhenParsed_ThenExceptionMentionsLineNumber() {
        InitCommandParser parser = new InitCommandParser();
        // line 1 valid, line 2 blank, line 3 malformed (no parentheses)
        InputStream input = toStream("$g = guest-entry();\n\nbroken-line;\n");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> parser.parse(input));
        assertTrue(ex.getMessage().contains("Line 3"),
                "Expected the error to point at line 3, but was: " + ex.getMessage());
    }

    // ── Executor tests: argument conversion failures ──────────────────────────

    @Test
    void GivenRegisterWithInvalidGroupDiscount_WhenExecuted_ThenThrows() {
        // POSH is not a valid UserGroupDiscount → valueOf throws
        ParsedCommand cmd = new ParsedCommand(null, "register",
                List.of("guestToken", "alice", "Alice", "pass", "a@a.com", "POSH"));

        assertThrows(RuntimeException.class, () -> executor.execute(cmd));
        verify(userService, never()).registerUser(anyString(), anyString(), anyString(),
                anyString(), any(), anyString());
    }

    @Test
    void GivenCreateEventWithNonNumericCapacity_WhenExecuted_ThenThrows() {
        // capacity "lots" cannot be parsed to int
        ParsedCommand cmd = new ParsedCommand("e1", "create-event",
                List.of("tok", "ev1", "1", "Rock Night", "lots",
                        "2026-07-11T20:00", "true", "Tel Aviv Arena"));

        assertThrows(RuntimeException.class, () -> executor.execute(cmd));
        verify(eventService, never()).createEvent(anyString(), any(), any(), any());
    }

    @Test
    void GivenCreateEventWithMalformedDate_WhenExecuted_ThenThrows() {
        // "next friday" is not an ISO LocalDateTime
        ParsedCommand cmd = new ParsedCommand("e1", "create-event",
                List.of("tok", "ev1", "1", "Rock Night", "500",
                        "next friday", "true", "Tel Aviv Arena"));

        assertThrows(RuntimeException.class, () -> executor.execute(cmd));
        verify(eventService, never()).createEvent(anyString(), any(), any(), any());
    }

    // ── Executor tests: variadic commands ─────────────────────────────────────

    @Test
    void GivenAppointManagerWithPermissions_WhenExecuted_ThenPermissionsParsedAndDelegated() {
        when(productionService.createProductionCompany(anyString(), any())).thenReturn(7);

        executor.execute(new ParsedCommand("c1", "create-production-company",
                List.of("tok", "Co", "Desc", "c@c.com")));

        // appoint-manager(token, companyId, managerId, perm1, perm2)
        executor.execute(new ParsedCommand(null, "appoint-manager",
                List.of("tok", "$c1", "bob",
                        "INVENTORY_MANAGEMENT", "SALES_REPORT_GENERATION")));

        verify(productionService).appointManager(
                eq("tok"), eq(7), eq("bob"),
                eq(EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT,
                        ManagerPermission.SALES_REPORT_GENERATION)));
    }

    @Test
    void GivenConfigureSeatingMapWithIncompleteTriplet_WhenExecuted_ThenThrows() {
        // triplets must come in groups of 3 after (token, eventId); here one value is missing
        ParsedCommand cmd = new ParsedCommand(null, "configure-event-seating-map",
                List.of("tok", "ev1", "10", "10", "120.0", "10"));

        assertThrows(RuntimeException.class, () -> executor.execute(cmd));
        verify(eventService, never()).configureSeatingMap(anyString(), any(), any());
    }

    @Test
    void GivenVariableBoundToVoidCommand_WhenReferencedLater_ThenThrowsUndefinedVariable() {
        // logout returns nothing, so "$x = logout(...)" must NOT store $x
        executor.execute(new ParsedCommand("x", "logout", List.of("alice", "tok")));

        ParsedCommand usesX = new ParsedCommand(null, "logout", List.of("alice", "$x"));

        assertThrows(RuntimeException.class, () -> executor.execute(usesX));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build an InitFileLoader with the mocked services and the given init file path,
     * using reflection to set the @Value field (no Spring context needed).
     */
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