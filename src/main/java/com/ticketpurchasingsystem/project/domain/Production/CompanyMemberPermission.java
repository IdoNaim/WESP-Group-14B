package com.ticketpurchasingsystem.project.domain.Production;

// Superseded by UserProductionCompany (users_production_companies table).
// Kept as a plain class so existing compilation units that reference it still compile.
public class CompanyMemberPermission {

    private Long id;
    private String userId;
    private ManagerPermission permission;

    protected CompanyMemberPermission() {}

    public CompanyMemberPermission(String userId, ManagerPermission permission, ProductionCompany company) {
        this.userId = userId;
        this.permission = permission;
    }

    public CompanyMemberPermission(Long id, String userId, ManagerPermission permission,
            ProductionCompany company) {
        this.id = id;
        this.userId = userId;
        this.permission = permission;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public ManagerPermission getPermission() { return permission; }
}
