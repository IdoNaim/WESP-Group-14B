package com.ticketpurchasingsystem.project.domain.Production;

// Superseded by UserProductionCompany (users_production_companies table).
// Kept as a plain class so existing compilation units that reference it still compile.
public class CompanyOwner {

    private Long id;
    private String userId;
    private String appointerId;

    protected CompanyOwner() {}

    public CompanyOwner(String userId, String appointerId, ProductionCompany company) {
        this.userId = userId;
        this.appointerId = appointerId;
    }

    public CompanyOwner(Long id, String userId, String appointerId, ProductionCompany company) {
        this.id = id;
        this.userId = userId;
        this.appointerId = appointerId;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getAppointerId() { return appointerId; }
}
