package com.ticketpurchasingsystem.project.domain.Utils;
public record PurchasePolicyDTO(
    Integer minTickets,
    Integer maxTickets,
    Integer minAge,
    Integer maxAge,
    boolean emnptySeatLeft
    //add routes 
) {}