package com.ticketpurchasingsystem.project.init;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class InitCommandParser {

    public List<ParsedCommand> parse(InputStream input) throws Exception {
        List<ParsedCommand> commands = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                commands.add(parseLine(line, lineNumber));
            }
        }

        return commands;
    }

    private ParsedCommand parseLine(String line, int lineNumber) {
        // strip trailing semicolon
        if (line.endsWith(";")) {
            line = line.substring(0, line.length() - 1).trim();
        }

        // check for variable assignment: $varName = command(...)
        String varName = null;
        if (line.startsWith("$")) {
            int eqIndex = line.indexOf('=');
            if (eqIndex == -1) {
                throw new RuntimeException("Line " + lineNumber + ": expected '=' in assignment: " + line);
            }
            varName = line.substring(1, eqIndex).trim();
            line = line.substring(eqIndex + 1).trim();
        }

        // parse: commandName(arg1, arg2, ...)
        int parenOpen = line.indexOf('(');
        int parenClose = line.lastIndexOf(')');

        if (parenOpen == -1 || parenClose == -1 || parenClose < parenOpen) {
            throw new RuntimeException("Line " + lineNumber + ": malformed command: " + line);
        }

        String name = line.substring(0, parenOpen).trim();
        String argString = line.substring(parenOpen + 1, parenClose).trim();

        List<String> args = new ArrayList<>();
        if (!argString.isEmpty()) {
            for (String arg : argString.split(",")) {
                args.add(arg.trim());
            }
        }

        return new ParsedCommand(varName, name, args);
    }
}
