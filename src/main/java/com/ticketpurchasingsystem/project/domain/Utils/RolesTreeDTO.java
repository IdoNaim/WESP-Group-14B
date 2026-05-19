package com.ticketpurchasingsystem.project.domain.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;

public class RolesTreeDTO {

    private static final String DIVIDER = "-----------------------";

    private Integer companyId;
    private String founderId;
    private Map<String, OwnerDTO> ownershipTree;
    private Map<String, ManagerDTO> managerTree;
    private Map<String, Set<ManagerPermission>> managerPermissions;

    public RolesTreeDTO(
            Integer companyId,
            String founderId,
            Map<String, OwnerDTO> ownershipTree,
            Map<String, ManagerDTO> managerTree,
            Map<String, Set<ManagerPermission>> managerPermissions) {
        this.companyId = companyId;
        this.founderId = founderId;
        this.ownershipTree = Collections.unmodifiableMap(ownershipTree);
        this.managerTree = Collections.unmodifiableMap(managerTree);
        this.managerPermissions = Collections.unmodifiableMap(managerPermissions);
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public String getFounderId() {
        return founderId;
    }

    public Map<String, OwnerDTO> getOwnershipTree() {
        return ownershipTree;
    }

    public Map<String, ManagerDTO> getManagerTree() {
        return managerTree;
    }

    public Map<String, Set<ManagerPermission>> getManagerPermissions() {
        return managerPermissions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(DIVIDER).append("\n");
        sb.append("company : ").append(companyId).append("\n");
        sb.append("founder : ").append(founderId).append("\n");
        sb.append(DIVIDER).append("\n");

        int ownerIndex = 1;
        for (Map.Entry<String, OwnerDTO> entry : ownershipTree.entrySet()) {
            String ownerId = entry.getKey();

            // collect everyone this owner appointed (other owners + managers)
            List<String> appointed = ownershipTree.entrySet().stream()
                    .filter(e -> ownerId.equals(e.getValue().getAppointerId()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(ArrayList::new));
            managerTree.entrySet().stream()
                    .filter(e -> ownerId.equals(e.getValue().getAppointerId()))
                    .map(Map.Entry::getKey)
                    .forEach(appointed::add);

            sb.append("owner").append(ownerIndex++).append(" : ").append(ownerId)
                    .append(" | appointed: ").append(appointed).append("\n");
        }

        sb.append(DIVIDER).append("\n");

        int managerIndex = 1;
        for (Map.Entry<String, ManagerDTO> entry : managerTree.entrySet()) {
            String managerId = entry.getKey();
            Set<ManagerPermission> perms = managerPermissions.getOrDefault(managerId, Collections.emptySet());
            sb.append("manager").append(managerIndex++).append(" : ").append(managerId)
                    .append(" | permissions: ").append(perms).append("\n");
        }

        sb.append(DIVIDER);
        return sb.toString();
    }
}