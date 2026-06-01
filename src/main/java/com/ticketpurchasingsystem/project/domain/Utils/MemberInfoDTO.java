package com.ticketpurchasingsystem.project.domain.Utils;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import java.util.Map;
import java.util.Set;

public class MemberInfoDTO {
    private String role;
    private Set<ManagerPermission> permissions;
    private String companyName;
    private String founderId;
    private Map<String, OwnerDTO> ownershipTree;
    private Map<String, ManagerDTO> managerTree;
    private Map<String, Set<ManagerPermission>> managerPermissions;

    public MemberInfoDTO(String role, Set<ManagerPermission> permissions, String companyName,
                         String founderId,
                         Map<String, OwnerDTO> ownershipTree,
                         Map<String, ManagerDTO> managerTree,
                         Map<String, Set<ManagerPermission>> managerPermissions) {
        this.role = role;
        this.permissions = permissions;
        this.companyName = companyName;
        this.founderId = founderId;
        this.ownershipTree = ownershipTree;
        this.managerTree = managerTree;
        this.managerPermissions = managerPermissions;
    }

    public String getRole() { return role; }
    public Set<ManagerPermission> getPermissions() { return permissions; }
    public String getCompanyName() { return companyName; }
    public String getFounderId() { return founderId; }
    public Map<String, OwnerDTO> getOwnershipTree() { return ownershipTree; }
    public Map<String, ManagerDTO> getManagerTree() { return managerTree; }
    public Map<String, Set<ManagerPermission>> getManagerPermissions() { return managerPermissions; }
}
