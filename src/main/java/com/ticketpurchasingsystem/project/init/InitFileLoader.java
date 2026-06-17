package com.ticketpurchasingsystem.project.init;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;


//ApplicationRunner is to make sure it run when you start it.
// the file is to just find the correct config file. that it.
// file system first , the classpath.
@Component
public class InitFileLoader implements ApplicationRunner {

    @Value("${init.file}")
    private String initFilePath;

    private final UserService userService;
    private final ProductionService productionService;
    private final EventService eventService;
    private final HistoryOrderService historyOrderService;
    private final ActiveOrderService activeOrderService;

    public InitFileLoader(UserService userService,
                          ProductionService productionService,
                          EventService eventService,
                          HistoryOrderService historyOrderService,
                          ActiveOrderService activeOrderService) {
        this.userService = userService;
        this.productionService = productionService;
        this.eventService = eventService;
        this.historyOrderService = historyOrderService;
        this.activeOrderService = activeOrderService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        InputStream input = resolveInput(initFilePath);

        if (input == null) {
            loggerDef.getInstance().warn("Init file not found: " + initFilePath + " — skipping initialization.");
            return;
        }

        loggerDef.getInstance().info("Loading init file: " + initFilePath);

        InitCommandParser parser = new InitCommandParser();
        List<ParsedCommand> commands = parser.parse(input);

        InitCommandExecutor executor = new InitCommandExecutor(
                userService, productionService, eventService, historyOrderService, activeOrderService);

        for (int i = 0; i < commands.size(); i++) {
            ParsedCommand cmd = commands.get(i);
            try {
                executor.execute(cmd);
                loggerDef.getInstance().info("Init [" + (i + 1) + "/" + commands.size() + "] OK: " + cmd.name());
            } catch (Exception e) {
                printInitError(initFilePath, cmd.name(), i + 1, commands.size(), rootMessage(e));
                throw new RuntimeException("Init failed at command [" + (i+1) + "/" + commands.size() + "]: " + cmd.name(), e);
            }
        }

        loggerDef.getInstance().info("System initialization complete. " + commands.size() + " commands executed.");
    }

    private static void printInitError(String file, String command, int index, int total, String reason) {
        String line1 = "  File   : " + file;
        String line2 = "  Command: " + command + "  [" + index + "/" + total + "]";
        String line3 = "  Error  : " + reason;
        int width = Math.max(60, Math.max(line1.length(), Math.max(line2.length(), line3.length())) + 2);
        String bar = "═".repeat(width);
        System.err.println("\n╔" + bar + "╗");
        System.err.println("║" + center("INITIALIZATION FAILED", width) + "║");
        System.err.println("╠" + bar + "╣");
        System.err.println("║" + pad(line1, width) + "║");
        System.err.println("║" + pad(line2, width) + "║");
        System.err.println("║" + pad(line3, width) + "║");
        System.err.println("╚" + bar + "╝\n");
    }

    private static String center(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - text.length() - padding);
    }

    private static String pad(String text, int width) {
        if (text.length() >= width) return text;
        return text + " ".repeat(width - text.length());
    }

    private static String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    // tries filesystem first, then classpath
    private InputStream resolveInput(String path) throws Exception {
        if (Files.exists(Paths.get(path))) {
            loggerDef.getInstance().info("Loading init file from filesystem: " + path);
            return new FileInputStream(path);
        }
        InputStream classpathStream = getClass().getClassLoader().getResourceAsStream(path);
        if (classpathStream != null) {
            loggerDef.getInstance().info("Loading init file from classpath: " + path);
        }
        return classpathStream;
    }
}
