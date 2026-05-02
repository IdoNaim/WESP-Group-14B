package com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy;

public class DiscountContext {
    private int ticketCount;
    private String couponCode;
    private double originalPrice;
    private double discountedPrice;

    public DiscountContext(int ticketCount, String couponCode, double originalPrice) {
        this.ticketCount = ticketCount;
        this.couponCode = couponCode;
        this.originalPrice = originalPrice;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public double getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(double discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

}