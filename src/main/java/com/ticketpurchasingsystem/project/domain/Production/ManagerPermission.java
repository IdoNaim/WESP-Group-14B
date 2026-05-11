package com.ticketpurchasingsystem.project.domain.Production;

import java.util.EnumSet;
import java.util.Set;

public enum ManagerPermission {
    INVENTORY_MANAGEMENT,
    VENUE_CONFIGURATION_AND_EVENT_MAPPING,
    COMPANY_POLICY_MANAGEMENT,
    PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT,
    CUSTOMER_INQUIRY_AND_RESPONSE_MANAGEMENT,
    PURCHASE_AND_ORDER_HISTORY_ACCESS,
    SALES_REPORT_GENERATION;

    public static Set<ManagerPermission> none() {
        return EnumSet.noneOf(ManagerPermission.class);
    }
}
