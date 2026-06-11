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

/**
 * Single join table that replaces the three separate tables for owners,
 * managers, and permissions.
 *
 * Each row is either a base membership row (permission = null) or a
 * permission-grant row (permission != null).
 *
 * Examples:
 *  (company_1, alice, OWNER,   null,    null)              → alice is an owner
 *  (company_1, bob,   MANAGER, alice,   null)              → bob is a manager appointed by alice
 *  (company_1, bob,   MANAGER, alice,   INVENTORY_MANAGEMENT) → bob's permission
 */
@Entity
@Table(name = "users_production_companies")
public class UserProductionCompany {

    public enum MemberRole { OWNER, MANAGER }

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
    @Column(name = "role", nullable = false, length = 50)
    private MemberRole role;

    @Column(name = "appointer_id", length = 255)
    private String appointerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 100)
    private ManagerPermission permission;

    protected UserProductionCompany() {}

    public UserProductionCompany(String userId, MemberRole role, String appointerId,
            ManagerPermission permission, ProductionCompany company) {
        this.userId = userId;
        this.role = role;
        this.appointerId = appointerId;
        this.permission = permission;
        this.company = company;
    }

    public UserProductionCompany(Long id, String userId, MemberRole role, String appointerId,
            ManagerPermission permission, ProductionCompany company) {
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.appointerId = appointerId;
        this.permission = permission;
        this.company = company;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public MemberRole getRole() { return role; }
    public String getAppointerId() { return appointerId; }
    public ManagerPermission getPermission() { return permission; }
    public ProductionCompany getCompany() { return company; }
    public void setCompany(ProductionCompany company) { this.company = company; }
}
