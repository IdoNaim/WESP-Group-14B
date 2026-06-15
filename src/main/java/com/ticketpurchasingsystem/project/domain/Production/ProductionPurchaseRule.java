package com.ticketpurchasingsystem.project.domain.Production;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "production_purchase_rules")
public class ProductionPurchaseRule {

    public enum RuleType { MIN_AGE, MAX_AGE, MIN_TICKETS, MAX_TICKETS, AND, OR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    @Column(name = "int_value")
    private Integer intValue;

    @Column(name = "position")
    private int position;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    @OrderBy("position ASC")
    private List<ProductionPurchaseRule> children = new ArrayList<>();

    protected ProductionPurchaseRule() {}

    public ProductionPurchaseRule(RuleType ruleType, Integer intValue, int position) {
        this.ruleType = ruleType;
        this.intValue = intValue;
        this.position = position;
        this.children = new ArrayList<>();
    }

    public void addChild(ProductionPurchaseRule child) {
        children.add(child);
    }

    public Long getId() { return id; }
    public RuleType getRuleType() { return ruleType; }
    public Integer getIntValue() { return intValue; }
    public int getPosition() { return position; }
    public List<ProductionPurchaseRule> getChildren() { return children; }
}
