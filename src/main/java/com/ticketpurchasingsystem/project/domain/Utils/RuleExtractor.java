// RuleExtractor.java
package com.ticketpurchasingsystem.project.domain.Utils;

import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.AndRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MaxAgeRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MaxTicketsRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.MinTicketsRule;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.OrRule;

public class RuleExtractor {

    Integer minAge, maxAge, minTickets, maxTickets;
    Boolean isAgeOr, isQuantityOr, isAgeAndQuantityOr;

    public void extract(IPurchaseRule rule, boolean isOr) {
        if (rule instanceof MinAgeRule r)       { minAge = r.getMinAge(); }
        else if (rule instanceof MaxAgeRule r)  { maxAge = r.getMaxAge(); }
        else if (rule instanceof MinTicketsRule r) { minTickets = r.getLimit(); }
        else if (rule instanceof MaxTicketsRule r) { maxTickets = r.getLimit(); }
        else if (rule instanceof OrRule r)      { extractBinary(r.getComponent1(), r.getComponent2(), true); }
        else if (rule instanceof AndRule r)     { extractBinary(r.getComponent1(), r.getComponent2(), false); }
    }

    private void extractBinary(IPurchaseRule left, IPurchaseRule right, boolean isOr) {
        boolean leftIsAge  = isAgeRule(left);
        boolean rightIsAge = isAgeRule(right);
        boolean leftIsQty  = isQuantityRule(left);
        boolean rightIsQty = isQuantityRule(right);

        if (leftIsAge && rightIsAge) {
            isAgeOr = isOr;
            extract(left, isOr);
            extract(right, isOr);
        } else if (leftIsQty && rightIsQty) {
            isQuantityOr = isOr;
            extract(left, isOr);
            extract(right, isOr);
        } else {
            // This is the top-level combinator between age block and quantity block
            isAgeAndQuantityOr = isOr;
            extract(left, isOr);
            extract(right, isOr);
        }
    }

    private boolean isAgeRule(IPurchaseRule r) {
        return r instanceof MinAgeRule || r instanceof MaxAgeRule
            || r instanceof AndRule a && isAgeRule(a.getComponent1())
            || r instanceof OrRule  o && isAgeRule(o.getComponent1());
    }

    private boolean isQuantityRule(IPurchaseRule r) {
        return r instanceof MinTicketsRule || r instanceof MaxTicketsRule
            || r instanceof AndRule a && isQuantityRule(a.getComponent1())
            || r instanceof OrRule  o && isQuantityRule(o.getComponent1());
    }

    public PurchasePolicyDTO toDTO() {
        return new PurchasePolicyDTO(
            minTickets,
            maxTickets,
            isQuantityOr  != null && isQuantityOr,
            minAge,
            maxAge,
            isAgeOr       != null && isAgeOr,
            isAgeAndQuantityOr != null && isAgeAndQuantityOr
        );
    }
}