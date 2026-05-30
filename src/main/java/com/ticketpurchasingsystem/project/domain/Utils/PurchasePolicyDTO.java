package com.ticketpurchasingsystem.project.domain.Utils;

public record PurchasePolicyDTO(
        // Quantity Rules Block
        Integer minTickets,
        Integer maxTickets,
        boolean isQuantityOr,        // false = AND, true = OR (e.g., max 2 OR min 100)

        // Age Rules Block
        Integer minAge,
        Integer maxAge,
        boolean isAgeOr,             // false = AND, true = OR

        // Outer Composition Block
        boolean isAgeAndQuantityOr  // false = AND, true = OR (Combines Age block with Quantity block)
) {}