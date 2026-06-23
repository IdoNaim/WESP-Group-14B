package com.ticketpurchasingsystem.project.domain.ProductionTest;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy.DiscountPolicy;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy.DiscountContext;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy.CouponDiscountRule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DiscountPolicyTest {

    @Test
    public void GivenDiscountContext_WhenSettingDiscountedPrice_ThenGettersReturnCorrectValues() {
        DiscountContext context = new DiscountContext(5, "SAVE20", 100.0);
        
        assertEquals(5, context.getTicketCount());
        assertEquals("SAVE20", context.getCouponCode());
        assertEquals(100.0, context.getOriginalPrice());
        
        context.setDiscountedPrice(80.0);
        assertEquals(80.0, context.getDiscountedPrice());
    }

    @Test
    public void GivenCouponDiscountRule_WhenApplyingWithMatchingAndNonMatchingCoupon_ThenReturnCorrectPrice() {
        CouponDiscountRule rule = new CouponDiscountRule("SAVE20", 20.0);
        
        // Scenario 1: Coupon matches
        DiscountContext contextMatch = new DiscountContext(1, "SAVE20", 100.0);
        double priceMatch = rule.applyDiscount(100.0, contextMatch);
        assertEquals(80.0, priceMatch);
        
        // Scenario 2: Coupon does not match
        DiscountContext contextNoMatch = new DiscountContext(1, "WRONGCODE", 100.0);
        double priceNoMatch = rule.applyDiscount(100.0, contextNoMatch);
        assertEquals(100.0, priceNoMatch);
    }

    @Test
    public void GivenDiscountPolicyWithMultipleRules_WhenCalculateFinalPrice_ThenApplyMatchingCouponDiscount() {
        DiscountPolicy policy = new DiscountPolicy();
        policy.addDiscountRule(new CouponDiscountRule("SAVE10", 10.0));
        policy.addDiscountRule(new CouponDiscountRule("SAVE20", 20.0));

        // Use context that matches the first coupon
        DiscountContext context1 = new DiscountContext(1, "SAVE10", 100.0);
        double finalPrice1 = policy.calculateFinalPrice(100.0, context1);
        assertEquals(90.0, finalPrice1);

        // Use context that matches the second coupon
        DiscountContext context2 = new DiscountContext(1, "SAVE20", 100.0);
        double finalPrice2 = policy.calculateFinalPrice(100.0, context2);
        assertEquals(80.0, finalPrice2);
    }
}
