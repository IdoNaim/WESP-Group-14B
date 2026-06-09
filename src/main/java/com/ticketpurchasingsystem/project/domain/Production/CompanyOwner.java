package com.ticketpurchasingsystem.project.domain.Production;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_owner")
public class CompanyOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "appointer_id", length = 255)
    private String appointerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private ProductionCompany company;

    protected CompanyOwner() {}

    public CompanyOwner(String userId, String appointerId, ProductionCompany company) {
        this.userId = userId;
        this.appointerId = appointerId;
        this.company = company;
    }

    public CompanyOwner(Long id, String userId, String appointerId, ProductionCompany company) {
        this.id = id;
        this.userId = userId;
        this.appointerId = appointerId;
        this.company = company;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getAppointerId() { return appointerId; }
    public ProductionCompany getCompany() { return company; }
    public void setCompany(ProductionCompany company) { this.company = company; }
}
