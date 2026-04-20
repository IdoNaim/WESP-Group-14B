package com.ticketpurchasingsystem.project.domain.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the discount policy attached to an Event.
 * Holds all discounts and defines how they combine.
 */
public class EventDiscountPolicy {

    private final List<Discount> discounts;
    private DiscountCompositionPolicy compositionPolicy;

    // Required by JPA/Hibernate
    protected EventDiscountPolicy() {
        this.discounts = new ArrayList<>();
    }

    public EventDiscountPolicy(DiscountCompositionPolicy compositionPolicy) {
        this.discounts = new ArrayList<>();
        this.compositionPolicy = compositionPolicy;
    }

    public void addDiscount(Discount discount) {
        discounts.add(discount);
        validateDiscountConflicts();
        validateTotalDiscountLimit();
    }

    public double calculateDiscount(Set<String> userDiscountGroups) {
        List<Discount> eligibleDiscounts = discounts.stream()
                .filter(d -> d.isEligible(userDiscountGroups))
                .collect(Collectors.toList());

        return compositionPolicy.apply(eligibleDiscounts);
    }

    private void validateDiscountConflicts() {
        Map<String, List<Discount>> grouped = discounts.stream()
                .collect(Collectors.groupingBy(Discount::getGroupId));

        for (List<Discount> group : grouped.values()) {
            long nonCombinableCount = group.stream()
                    .filter(d -> !d.isCombinable())
                    .count();

            if (nonCombinableCount > 1) {
                throw new IllegalArgumentException("Conflicting non-combinable discounts in same group");
            }
        }
    }

    private void validateTotalDiscountLimit() {
        if (compositionPolicy == DiscountCompositionPolicy.SUM) {
            // Validation only applies to combinable discounts
            double totalCombinable = discounts.stream()
                    .filter(Discount::isCombinable)
                    .mapToDouble(Discount::getPercentage)
                    .sum();

            if (totalCombinable > 100) {
                throw new IllegalArgumentException("Total combinable discount exceeds 100%");
            }
        }
    }

    public List<Discount> getDiscounts() {
        return Collections.unmodifiableList(discounts);
    }

    public void setCompositionPolicy(DiscountCompositionPolicy policy) {
        this.compositionPolicy = policy;
    }
}


/**
 * Defines how multiple discounts combine.
 */
enum DiscountCompositionPolicy {

    MAX {
        @Override
        public double apply(List<Discount> discounts) {
            return discounts.stream()
                    .mapToDouble(Discount::getPercentage)
                    .max()
                    .orElse(0);
        }
    },

    SUM {
        @Override
        public double apply(List<Discount> discounts) {
            // FIX: Handle combinable vs non-combinable logic properly
            double maxNonCombinable = discounts.stream()
                    .filter(d -> !d.isCombinable())
                    .mapToDouble(Discount::getPercentage)
                    .max()
                    .orElse(0);

            double sumCombinable = discounts.stream()
                    .filter(Discount::isCombinable)
                    .mapToDouble(Discount::getPercentage)
                    .sum();

            // The user gets the best deal between their max non-combinable and the sum of their combinable
            double total = Math.max(maxNonCombinable, sumCombinable);
            
            // Cap at 100% instead of throwing error during user purchase flow
            return Math.min(total, 100.0);
        }
    };

    public abstract double apply(List<Discount> discounts);
}


/**
 * Represents a single discount rule inside Event aggregate.
 */
class Discount {

    private double percentage;
    private String targetGroup;
    private boolean combinable;
    private String groupId;

    // Required by JPA/Hibernate
    protected Discount() {}

    public Discount(double percentage, String targetGroup, boolean combinable, String groupId) {
        if (percentage <= 0 || percentage > 100) {
            throw new IllegalArgumentException("Invalid discount percentage");
        }
        this.percentage = percentage;
        this.targetGroup = targetGroup;
        this.combinable = combinable;
        this.groupId = groupId;
    }

    public boolean isEligible(Set<String> userDiscountGroups) {
        return userDiscountGroups.contains(targetGroup);
    }

    public double getPercentage() { return percentage; }
    public boolean isCombinable() { return combinable; }
    public String getGroupId() { return groupId; }
}