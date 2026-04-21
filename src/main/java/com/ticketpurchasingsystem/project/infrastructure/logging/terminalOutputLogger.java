package com.ticketpurchasingsystem.project.infrastructure.logging;

public class terminalOutputLogger extends outputLogger {

        @Override
        public void log(String msg) {
            System.out.println(msg);
        }
}
