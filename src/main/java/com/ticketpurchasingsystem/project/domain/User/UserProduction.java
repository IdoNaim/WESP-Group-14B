package com.ticketpurchasingsystem.project.domain.User;

import java.util.HashMap;
import java.util.Map;

public class UserProduction {
    public enum RoleInProduction {
        FOUNDER,
        OWNER,
        MANAGER
    }

    private final Map<Integer, RoleInProduction> productions;

    public UserProduction() {
        this.productions = new HashMap<>();
    }

    public void addProduction(Integer productionId, RoleInProduction roleInProduction) {
        productions.put(productionId, roleInProduction);
    }

    public RoleInProduction getProductionRole(Integer productionId) {
        if (!productions.containsKey(productionId)) {
            throw new IllegalArgumentException("User is not part of this production.");
        }
        return productions.get(productionId);
    }

    public Map<Integer, RoleInProduction> getAllProductions() {
        return new HashMap<>(productions);
    }

    public void removeProduction(Integer productionId) {
        if (!productions.containsKey(productionId)) {
            throw new IllegalArgumentException("User is not part of this production.");
        }
        productions.remove(productionId);
    }
}
