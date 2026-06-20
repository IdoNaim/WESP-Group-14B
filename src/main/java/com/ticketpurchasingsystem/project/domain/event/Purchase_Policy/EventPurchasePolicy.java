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
        rules.clear();

        IPurchaseRule ageBlock = null;
        if (minAge != null && maxAge != null) {
            IPurchaseRule minA = new MinAgeRule(minAge);
            IPurchaseRule maxA = new MaxAgeRule(maxAge);
            ageBlock = (isAgeOr != null && isAgeOr) ? new OrRule(minA, maxA) : new AndRule(minA, maxA);
        } else if (minAge != null) {
            ageBlock = new MinAgeRule(minAge);
        } else if (maxAge != null) {
            ageBlock = new MaxAgeRule(maxAge);
        }

        IPurchaseRule quantityBlock = null;
        if (minTickets != null && maxTickets != null) {
            IPurchaseRule minT = new MinTicketsRule(minTickets);
            IPurchaseRule maxT = new MaxTicketsRule(maxTickets);
            quantityBlock = (isQuantityOr != null && isQuantityOr) ? new OrRule(minT, maxT) : new AndRule(minT, maxT);
        } else if (minTickets != null) {
            quantityBlock = new MinTicketsRule(minTickets);
        } else if (maxTickets != null) {
            quantityBlock = new MaxTicketsRule(maxTickets);
        }

        if (ageBlock != null && quantityBlock != null) {
            IPurchaseRule root = (isAgeAndQuantityOr != null && isAgeAndQuantityOr)
                    ? new OrRule(ageBlock, quantityBlock)
                    : new AndRule(ageBlock, quantityBlock);
            rules.add(root);
        } else if (ageBlock != null) {
            rules.add(ageBlock);
        } else if (quantityBlock != null) {
            rules.add(quantityBlock);
        }
    }

    // Called automatically by Hibernate before saving to extract rules to flat columns
//    @PrePersist
//    @PreUpdate
//    private void extractRulesForDb() {
//        PurchasePolicyDTO dto = getDTO();
//        this.maxTickets = dto.maxTickets();
//        this.minTickets = dto.minTickets();
//        this.maxAge = dto.maxAge();
//        this.minAge = dto.minAge();
//        this.isQuantityOr = dto.isQuantityOr();
//        this.isAgeOr = dto.isAgeOr();
//        this.isAgeAndQuantityOr = dto.isAgeAndQuantityOr();
//    }

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
    public void updateFromDTO(PurchasePolicyDTO dto) {
        this.minTickets = dto.minTickets();
        this.maxTickets = dto.maxTickets();
        this.minAge = dto.minAge();
        this.maxAge = dto.maxAge();
        this.isQuantityOr = dto.isQuantityOr();
        this.isAgeOr = dto.isAgeOr();
        this.isAgeAndQuantityOr = dto.isAgeAndQuantityOr();

        rebuildRulesFromDb();
    }
//    public PurchasePolicyDTO getDTO() {
//        if (rules.isEmpty()) {
//            return new PurchasePolicyDTO(null, null, false, null, null, false, false);
//        }
//        RuleExtractor extractor = new RuleExtractor();
//        extractor.extract(rules.get(0), false);
//        return extractor.toDTO();
//    }
    public PurchasePolicyDTO getDTO() {
        return new PurchasePolicyDTO(
                minTickets, maxTickets,
                isQuantityOr != null && isQuantityOr,
                minAge, maxAge,
                isAgeOr != null && isAgeOr,
                isAgeAndQuantityOr != null && isAgeAndQuantityOr
        );
    }
}