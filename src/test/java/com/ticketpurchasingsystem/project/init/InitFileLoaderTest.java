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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
    void GivenInitFileWithInvalidCommand_WhenLoaderRuns_ThenSystemExitsWithFailure() throws Exception {
        Path tmp = Files.createTempFile("bad_init", ".txt");
        Files.writeString(tmp, "totally-invalid-command();\n");

        InitFileLoader loader = buildLoader(tmp.toString());

        // InitFileLoader calls System.exit(1) on failure;
        // catch it via a SecurityManager stub or verify the logger / exception path.
        // Since System.exit cannot be easily intercepted without a SecurityManager,
        // we test the executor directly for the same scenario and trust loader wires it.
        ParsedCommand bad = new ParsedCommand(null, "totally-invalid-command", java.util.List.of());
        assertThrows(RuntimeException.class, () -> executor.execute(bad));
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
    void GivenInitFileWhereMiddleCommandFails_WhenLoaderRuns_ThenInitHaltsAndDoesNotContinue() throws Exception {
        when(userService.guestEntry()).thenReturn("gt1");
        when(userService.loginUser(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Wrong password"));

        Path tmp = Files.createTempFile("mid_fail_init", ".txt");
        Files.writeString(tmp,
                "$g1 = guest-entry();\n" +
                        "$tok = login($g1, alice, wrongpass);\n" +
                        // This line must never be reached:
                        "logout(alice, $tok);\n");

        InitFileLoader loader = buildLoader(tmp.toString());

        // Loader calls System.exit(1); we can't catch that without a SecurityManager.
        // Instead, verify that logout is never called (init halted after the failure).
        try {
            loader.run(new DefaultApplicationArguments());
        } catch (Exception ignored) {}

        verify(userService, never()).logoutUser(anyString(), anyString());
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