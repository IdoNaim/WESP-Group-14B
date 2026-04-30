package com.ticketpurchasingsystem.project.domain.User;

import java.util.HashMap;
import java.util.Map;

public class UserProduction {
    private final Map<String, String> productions;

    public UserProduction() {
        this.productions = new HashMap<>();
    }

    public void addProduction(String productionId, String roleInProduction) {
        productions.put(productionId, roleInProduction);
    }

    public String getProductionRole(String productionId) {
        if (!productions.containsKey(productionId)) {
            return "User is not part of this production.";
        }
        return productions.get(productionId);
    }

    public Map<String, String> getAllProductions() {
        return new HashMap<>(productions);
    }

    public void removeProduction(String productionId) {
        if (!productions.containsKey(productionId)) {
            throw new IllegalArgumentException("User is not part of this production.");
        }
        productions.remove(productionId);
    }
}
