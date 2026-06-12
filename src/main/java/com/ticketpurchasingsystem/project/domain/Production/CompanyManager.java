package com.ticketpurchasingsystem.project.domain.Production;

// Superseded by UserProductionCompany (users_production_companies table).
// Kept as a plain class so existing compilation units that reference it still compile.
public class CompanyManager {

    private Long id;
    private String userId;
    private String appointerId;

    protected CompanyManager() {}

    public CompanyManager(String userId, String appointerId, ProductionCompany company) {
        this.userId = userId;
        this.appointerId = appointerId;
    }

    public CompanyManager(Long id, String userId, String appointerId, ProductionCompany company) {
        this.id = id;
        this.userId = userId;
        this.appointerId = appointerId;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getAppointerId() { return appointerId; }
}
