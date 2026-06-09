package com.ticketpurchasingsystem.project.domain.Production;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "company_member_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "user_id", "permission"}))
public class CompanyMemberPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private ProductionCompany company;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 100)
    private ManagerPermission permission;

    protected CompanyMemberPermission() {}

    public CompanyMemberPermission(String userId, ManagerPermission permission, ProductionCompany company) {
        this.userId = userId;
        this.permission = permission;
        this.company = company;
    }

    public CompanyMemberPermission(Long id, String userId, ManagerPermission permission,
            ProductionCompany company) {
        this.id = id;
        this.userId = userId;
        this.permission = permission;
        this.company = company;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public ManagerPermission getPermission() { return permission; }
    public ProductionCompany getCompany() { return company; }
    public void setCompany(ProductionCompany company) { this.company = company; }
}
