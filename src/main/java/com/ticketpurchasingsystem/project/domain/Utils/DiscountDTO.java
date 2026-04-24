package com.ticketpurchasingsystem.project.domain.Utils;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Discount Policies.
 * Using a Record for immutability and concise syntax.
 */
public record DiscountDTO(
    String type,               // e.g., "VISIBLE", "DEPENDENT", "COUPON"
    String name,
    
    // Fields for Visible/Coupon
    Double percentage,
    LocalDateTime validUntil,
    String couponCode,
    
    // Fields for Dependent
    Integer haveToBuyAmount,
    Integer getForFreeAmount
) {}