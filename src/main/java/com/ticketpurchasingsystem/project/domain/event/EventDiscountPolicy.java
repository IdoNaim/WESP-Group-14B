package com.ticketpurchasingsystem.project.domain.event;
import java.util.ArrayList;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Utils.DiscountDTO;
public class EventDiscountPolicy {
    ArrayList<Discount> discounts;
    public EventDiscountPolicy(List<DiscountDTO> discountDTOs) {
        this.discounts = new ArrayList<>();
        for (DiscountDTO dto : discountDTOs) {
            if(dto.type().equals("VISIBLE")) {
                discounts.add(new VisibleDiscount(dto.name(), dto.percentage(), dto.validUntil()));
            } else if (dto.type().equals("DEPENDENT")) {
                discounts.add(new DependentDiscount(dto.name(), dto.haveToBuyAmount(), dto.getForFreeAmount()));
            } else if (dto.type().equals("COUPON")) {
                discounts.add( new CouponDiscount( dto.name(), dto.couponCode(), dto.percentage(), dto.validUntil()));
            }
        }
    }



    
}

