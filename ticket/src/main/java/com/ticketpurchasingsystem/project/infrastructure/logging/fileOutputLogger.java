package com.ticketpurchasingsystem.project.infrastructure.logging;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class fileOutputLogger extends outputLogger {

    @Override
    public void log(String msg) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("log.txt", true))) {
            writer.write(msg);
            writer.newLine();
        } catch (Exception e) {
            throw new RuntimeException("Failed to write log to file", e);
        }
    }

    
}
