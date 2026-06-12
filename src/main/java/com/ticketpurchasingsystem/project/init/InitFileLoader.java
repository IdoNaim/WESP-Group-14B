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

import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Component
public class InitFileLoader implements ApplicationRunner {

    @Value("${init.file}")
    private String initFilePath;

    private final UserService userService;
    private final ProductionService productionService;
    private final EventService eventService;
    private final HistoryOrderService historyOrderService;

    public InitFileLoader(UserService userService,
                          ProductionService productionService,
                          EventService eventService,
                          HistoryOrderService historyOrderService) {
        this.userService = userService;
        this.productionService = productionService;
        this.eventService = eventService;
        this.historyOrderService = historyOrderService;
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
                userService, productionService, eventService, historyOrderService);

        for (int i = 0; i < commands.size(); i++) {
            ParsedCommand cmd = commands.get(i);
            try {
                executor.execute(cmd);
                loggerDef.getInstance().info("Init [" + (i + 1) + "/" + commands.size() + "] OK: " + cmd.name());
            } catch (Exception e) {
                String msg = "Init failed at command " + (i + 1) + " (" + cmd.name() + "): " + e.getMessage();
                loggerDef.getInstance().error(msg);
                throw new RuntimeException(msg, e);
            }
        }

        loggerDef.getInstance().info("System initialization complete. " + commands.size() + " commands executed.");
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
