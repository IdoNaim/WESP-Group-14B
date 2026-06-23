package com.ticketpurchasingsystem.project.domain.Events;

import com.ticketpurchasingsystem.project.domain.event.Discounts.CouponDiscount;
import com.ticketpurchasingsystem.project.domain.event.Discounts.DependentDiscount;
import com.ticketpurchasingsystem.project.domain.event.Discounts.VisibleDiscount;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class EventDiscountsTest {

    @Test
    public void GivenVisibleDiscountWithFutureDate_WhenCalculateDiscount_ThenReturnCorrectAmount() {
        LocalDateTime validUntil = LocalDateTime.now().plusDays(10);
        VisibleDiscount discount = new VisibleDiscount("Summer Sale", 15.5, validUntil);
        
        assertEquals("Summer Sale", discount.getDiscountName());
        assertTrue(discount.isApplicable());
        assertEquals(15.5, discount.calculateDiscountAmount());
    }

    @Test
    public void GivenCouponDiscountWithFutureDate_WhenCalculateDiscount_ThenReturnCorrectAmount() {
        LocalDateTime validUntil = LocalDateTime.now().plusDays(5);
        CouponDiscount discount = new CouponDiscount("Promo Code", "SAVE10", 10.0, validUntil);

        assertEquals("Promo Code", discount.getDiscountName());
        assertTrue(discount.isApplicable());
        assertEquals(4.6, discount.calculateDiscountAmount());
    }

    @Test
    public void GivenDependentDiscountWithBuyTwoGetOne_WhenCalculateDiscount_ThenReturnCorrectAmount() {
        DependentDiscount discount = new DependentDiscount("Buy 2 Get 1", 2, 1);

        assertEquals("Buy 2 Get 1", discount.getDiscountName());
        assertTrue(discount.isApplicable());
        assertEquals(5.6, discount.calculateDiscountAmount());
    }
}
