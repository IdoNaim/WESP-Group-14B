package com.ticketpurchasingsystem.project.domain.event.Purchase_Policy;

import com.ticketpurchasingsystem.project.domain.Utils.RuleExtractor;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "EventPurchasePolicies")
public class EventPurchasePolicy implements IPurchaseRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id", updatable = false, nullable = false)
    private String eventId; // Shares PK/FK with Event

    // Database mapped columns (flat representation)
    @Column(name = "MaxTickets") private Integer maxTickets;
    @Column(name = "MinTickets") private Integer minTickets;
    @Column(name = "MaxAge") private Integer maxAge;
    @Column(name = "MinAge") private Integer minAge;
    @Column(name = "isQuantityOr") private Boolean isQuantityOr;
    @Column(name = "isAgeOr") private Boolean isAgeOr;
    @Column(name = "isAgeAndQuantityOr") private Boolean isAgeAndQuantityOr;

    // Ignored by the database, used strictly for memory business logic
    @Transient
    private final List<IPurchaseRule> rules;

    public EventPurchasePolicy() {
        this.rules = new ArrayList<>();
    }

    // Called automatically by Hibernate after fetching from DB to rebuild the composite tree
    @PostLoad
    private void rebuildRulesFromDb() {
        PurchasePolicyDTO dto = new PurchasePolicyDTO(
                minTickets, maxTickets,
                isQuantityOr != null && isQuantityOr,
                minAge, maxAge,
                isAgeOr != null && isAgeOr,
                isAgeAndQuantityOr != null && isAgeAndQuantityOr
        );
        // Assuming your Event class has the logic to convert DTO -> rules,
        // you would apply that logic here to repopulate 'this.rules'.
    }

    // Called automatically by Hibernate before saving to extract rules to flat columns
    @PrePersist
    @PreUpdate
    private void extractRulesForDb() {
        PurchasePolicyDTO dto = getDTO();
        this.maxTickets = dto.maxTickets();
        this.minTickets = dto.minTickets();
        this.maxAge = dto.maxAge();
        this.minAge = dto.minAge();
        this.isQuantityOr = dto.isQuantityOr();
        this.isAgeOr = dto.isAgeOr();
        this.isAgeAndQuantityOr = dto.isAgeAndQuantityOr();
    }

    public void addRule(IPurchaseRule rule) {
        if (rule != null) { rules.add(rule); }
    }

    public void removeRule(IPurchaseRule rule) {
        rules.remove(rule);
    }

    @Override
    public boolean validate(PurchaseContext context) {
        for (IPurchaseRule rule : rules) {
            if (!rule.validate(context)) { return false; }
        }
        return true;
    }

    public PurchasePolicyDTO getDTO() {
        if (rules.isEmpty()) {
            return new PurchasePolicyDTO(null, null, false, null, null, false, false);
        }
        RuleExtractor extractor = new RuleExtractor();
        extractor.extract(rules.get(0), false);
        return extractor.toDTO();
    }
}