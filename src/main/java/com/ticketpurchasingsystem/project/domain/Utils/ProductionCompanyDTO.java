package com.ticketpurchasingsystem.project.domain.Utils;

public class ProductionCompanyDTO {
    private String companyName;
    private String companyDescription;
    private String companyEmail;

    public ProductionCompanyDTO(String companyName, String companyDescription, String companyEmail) {
        this.companyName = companyName;
        this.companyDescription = companyDescription;
        this.companyEmail = companyEmail;
    }

    public ProductionCompanyDTO() {
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }
}
