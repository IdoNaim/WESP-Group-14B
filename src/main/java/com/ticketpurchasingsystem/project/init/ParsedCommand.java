package com.ticketpurchasingsystem.project.init;

import java.util.List;

public record ParsedCommand(String varName, String name, List<String> args) {
    public boolean hasVar() {
        return varName != null;
    }
}
