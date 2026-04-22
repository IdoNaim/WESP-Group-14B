package com.ticketpurchasingsystem.project.infrastructure.logging;

public class loggerDef extends logger {
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final outputLogger DEFAULT_OUTPUT_LOGGER = new terminalOutputLogger(); 
    private static final loggerDef INSTANCE = new loggerDef();

    private loggerDef() {
        this.out = DEFAULT_OUTPUT_LOGGER;
    }
    public void setOutputLoggerFile(outputLogger newOut) {
        this.out = new fileOutputLogger();
    }
    public void setOutputTerminal(outputLogger newOut) {
        this.out = new terminalOutputLogger(); 
    }

    public static loggerDef getInstance() {
        return INSTANCE;
    }
    public String formatMsg(String message) {
        String timestamp = "[" + java.time.LocalDateTime.now() + "]";
        return CYAN + timestamp + RESET + " " + message;
    }

    public void warn(String message) {
        log(logLevel.WARNING, formatMsg(message));
    }

    public void error(String message) {
        log(logLevel.ERROR, formatMsg(message));
    }

    public void info(String message) {
        log(logLevel.INFO, formatMsg(message));
    }
    private void log(logLevel level, String message) {
        String coloredMessage = switch (level) {
            case INFO -> BLUE + message + RESET;
            case WARNING -> YELLOW + message + RESET;
            case ERROR -> RED + message + RESET;
        };
        out.log(coloredMessage);
    }
}
