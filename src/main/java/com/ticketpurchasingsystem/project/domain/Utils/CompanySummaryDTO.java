package com.ticketpurchasingsystem.project.domain.Utils;

public class CompanySummaryDTO {
    private Integer companyId;
    private String companyName;
    private String companyDescription;
    private String companyEmail;
    private String role;

    public CompanySummaryDTO(Integer companyId, String companyName, String companyDescription, String companyEmail, String role) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.companyDescription = companyDescription;
        this.companyEmail = companyEmail;
        this.role = role;
    }

    public Integer getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public String getCompanyDescription() { return companyDescription; }
    public String getCompanyEmail() { return companyEmail; }
    public String getRole() { return role; }
}
