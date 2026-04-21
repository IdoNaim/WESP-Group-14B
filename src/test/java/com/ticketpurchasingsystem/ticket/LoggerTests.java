package com.ticketpurchasingsystem.ticket;

import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;
import com.ticketpurchasingsystem.project.infrastructure.logging.outputLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ticketpurchasingsystem.project.infrastructure.logging.fileOutputLogger;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LoggerTests {

    private static final String ESC    = "";
    private static final String BLUE   = ESC + "[34m";
    private static final String YELLOW = ESC + "[33m";
    private static final String RED    = ESC + "[31m";
    private static final String RESET  = ESC + "[0m";
    private static final String CYAN   = ESC + "[36m";

    private loggerDef logger;
    private List<String> capturedLogs;

    @BeforeEach
    void setUp() throws Exception {
        logger = loggerDef.getInstance();
        capturedLogs = new ArrayList<>();

        outputLogger capturer = new outputLogger() {
            @Override
            public void log(String msg) {
                capturedLogs.add(msg);
            }
        };

        // logger.out is defined on the abstract parent class
        Field outField = logger.getClass().getSuperclass().getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(logger, capturer);
    }

    @Test
    void singletonReturnsSameInstance() {
        assertSame(loggerDef.getInstance(), loggerDef.getInstance());
    }

    @Test
    void formatMsgContainsOriginalMessage() {
        String result = logger.formatMsg("hello world");
        assertTrue(result.contains("hello world"));
    }

    @Test
    void formatMsgContainsCurrentDate() {
        String result = logger.formatMsg("msg");
        assertTrue(result.contains(LocalDate.now().toString()),
                "Timestamp should contain today's date: " + LocalDate.now());
    }

    @Test
    void formatMsgStartsWithCyanColor() {
        String result = logger.formatMsg("msg");
        assertTrue(result.startsWith(CYAN));
    }

    @Test
    void infoLogsWithBlueColor() {
        logger.info("test info");
        assertEquals(1, capturedLogs.size());
        String logged = capturedLogs.get(0);
        assertTrue(logged.startsWith(BLUE), "INFO message should start with blue ANSI code");
        assertTrue(logged.contains("test info"));
        assertTrue(logged.endsWith(RESET));
    }

    @Test
    void warnLogsWithYellowColor() {
        logger.warn("test warning");
        assertEquals(1, capturedLogs.size());
        String logged = capturedLogs.get(0);
        assertTrue(logged.startsWith(YELLOW), "WARN message should start with yellow ANSI code");
        assertTrue(logged.contains("test warning"));
        assertTrue(logged.endsWith(RESET));
    }

    @Test
    void errorLogsWithRedColor() {
        logger.error("test error");
        assertEquals(1, capturedLogs.size());
        String logged = capturedLogs.get(0);
        assertTrue(logged.startsWith(RED), "ERROR message should start with red ANSI code");
        assertTrue(logged.contains("test error"));
        assertTrue(logged.endsWith(RESET));
    }

    @Test
    void eachLogCallInvokesOutputLoggerOnce() {
        logger.info("a");
        logger.warn("b");
        logger.error("c");
        assertEquals(3, capturedLogs.size());
    }

    @Test
    void emptyMessageIsLoggedWithoutError() {
        assertDoesNotThrow(() -> logger.info(""));
        assertEquals(1, capturedLogs.size());
    }

    // --- fileOutputLogger tests ---

    private static final Path LOG_FILE = Path.of("log.txt");

    @AfterEach
    void cleanUpLogFile() throws IOException {
        Files.deleteIfExists(LOG_FILE);
    }

    @Test
    void fileOutputLoggerWritesMessageToFile() throws IOException {
        fileOutputLogger fileLogger = new fileOutputLogger();
        fileLogger.log("file test message");
        String content = Files.readString(LOG_FILE);
        assertTrue(content.contains("file test message"));
    }

    @Test
    void fileOutputLoggerAppendsMultipleMessages() throws IOException {
        fileOutputLogger fileLogger = new fileOutputLogger();
        fileLogger.log("first");
        fileLogger.log("second");
        String content = Files.readString(LOG_FILE);
        assertTrue(content.contains("first"));
        assertTrue(content.contains("second"));
    }

    @Test
    void fileOutputLoggerCreatesFileIfAbsent() throws IOException {
        Files.deleteIfExists(LOG_FILE);
        fileOutputLogger fileLogger = new fileOutputLogger();
        assertDoesNotThrow(() -> fileLogger.log("create file"));
        assertTrue(Files.exists(LOG_FILE));
    }

    // --- output switching tests ---

    @Test
    void setOutputLoggerFileWritesToFile() throws IOException {
        logger.setOutputLoggerFile(null);
        logger.info("switched to file");
        assertTrue(Files.exists(LOG_FILE), "log.txt should be created after switching to file output");
        assertTrue(Files.readString(LOG_FILE).contains("switched to file"));
    }

    @Test
    void setOutputLoggerFileStopsWritingToCapturer() {
        logger.setOutputLoggerFile(null);
        logger.info("should not reach capturer");
        assertTrue(capturedLogs.isEmpty(), "capturer should receive nothing after switching to file output");
    }

    @Test
    void setOutputTerminalStopsWritingToFile() throws IOException {
        logger.setOutputLoggerFile(null);
        logger.setOutputTerminal(null);
        logger.info("back to terminal");
        assertFalse(Files.exists(LOG_FILE), "log.txt should not be written after switching back to terminal");
    }

    @Test
    void switchFromFileToTerminalAndBackWritesToFile() throws IOException {
        logger.setOutputLoggerFile(null);
        logger.setOutputTerminal(null);
        logger.setOutputLoggerFile(null);
        logger.info("re-switched to file");
        assertTrue(Files.readString(LOG_FILE).contains("re-switched to file"));
    }
}
