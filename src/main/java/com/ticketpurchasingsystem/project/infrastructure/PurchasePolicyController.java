package com.ticketpurchasingsystem.project.infrastructure;

import com.ticketpurchasingsystem.project.application.PurchasePolicyService;
import com.ticketpurchasingsystem.project.domain.tickets.*;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PurchasePolicyController {

    private final PurchasePolicyService purchasePolicyService;

    public PurchasePolicyController(PurchasePolicyService purchasePolicyService) {
        this.purchasePolicyService = purchasePolicyService;
    }

    @PostMapping("/validate")
    public ResponseEntity<PolicyValidationResult> validatePurchase(@RequestBody ValidationRequest request) {
        PolicyValidationResult result = purchasePolicyService.validatePurchase(
                request.getEventId(),
                request.getBuyerAge(),
                request.getRequestedTickets()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/assign/event/{eventId}")
    public ResponseEntity<String> assignPolicyToEvent(@PathVariable String eventId, @RequestBody PolicyRequest request) {
        try {
            ITicketPurchaseRule rule = buildRule(request);
            purchasePolicyService.assignPolicyToEvent(eventId, rule);
            return ResponseEntity.ok("Policy assigned to event successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/assign/company/{companyId}")
    public ResponseEntity<String> assignPolicyToCompany(@PathVariable Integer companyId, @RequestBody PolicyRequest request) {
        try {
            ITicketPurchaseRule rule = buildRule(request);
            purchasePolicyService.assignPolicyToCompany(companyId, rule);
            return ResponseEntity.ok("Policy assigned to company successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<PolicyResponse> getPolicyByEvent(@PathVariable String eventId) {
        try {
            ITicketPurchaseRule rule = purchasePolicyService.getPolicyByEvent(eventId);
            return ResponseEntity.ok(new PolicyResponse(describeRule(rule)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<PolicyResponse> getPolicyByCompany(@PathVariable Integer companyId) {
        try {
            ITicketPurchaseRule rule = purchasePolicyService.getPolicyByCompany(companyId);
            return ResponseEntity.ok(new PolicyResponse(describeRule(rule)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ITicketPurchaseRule buildRule(PolicyRequest req) {
        if (req == null || req.getType() == null) {
            return null;
        }
        switch (req.getType().toUpperCase()) {
            case "AGE":
                if (req.getMinAge() != null && req.getMaxAge() != null && req.getMinAge() > req.getMaxAge()) {
                    throw new IllegalArgumentException(
                            "minAge (" + req.getMinAge() + ") cannot be greater than maxAge (" + req.getMaxAge() + ")");
                }
                IPurchaseRule ageRule;
                if (req.getMinAge() != null && req.getMaxAge() != null) {
                    ageRule = new AndRule(new MinAgeRule(req.getMinAge()), new MaxAgeRule(req.getMaxAge()));
                } else if (req.getMinAge() != null) {
                    ageRule = new MinAgeRule(req.getMinAge());
                } else if (req.getMaxAge() != null) {
                    ageRule = new MaxAgeRule(req.getMaxAge());
                } else {
                    throw new IllegalArgumentException("At least one of minAge or maxAge is required for AGE policy");
                }
                return new PurchaseRuleAdapter(ageRule, "AGE", req.getMinAge(), req.getMaxAge(), null, null, null);
            case "MIN_TICKETS":
                if (req.getMinTickets() == null) {
                    throw new IllegalArgumentException("minTickets is required for MIN_TICKETS policy");
                }
                return new PurchaseRuleAdapter(new MinTicketsRule(req.getMinTickets()), "MIN_TICKETS", null, null, req.getMinTickets(), null, null);
            case "MAX_TICKETS":
                if (req.getMaxTickets() == null) {
                    throw new IllegalArgumentException("maxTickets is required for MAX_TICKETS policy");
                }
                return new PurchaseRuleAdapter(new MaxTicketsRule(req.getMaxTickets()), "MAX_TICKETS", null, null, null, req.getMaxTickets(), null);
            case "AND":
                if (req.getSubPolicies() == null) {
                    throw new IllegalArgumentException("subPolicies are required for AND composition");
                }
                // Cross-validate direct children for logically impossible constraints
                Integer andMinTickets = null, andMaxTickets = null, andMinAge = null, andMaxAge = null;
                for (PolicyRequest sub : req.getSubPolicies()) {
                    if (sub.getType() == null) continue;
                    switch (sub.getType().toUpperCase()) {
                        case "MIN_TICKETS": andMinTickets = sub.getMinTickets(); break;
                        case "MAX_TICKETS": andMaxTickets = sub.getMaxTickets(); break;
                        case "AGE":
                            if (sub.getMinAge() != null) andMinAge = sub.getMinAge();
                            if (sub.getMaxAge() != null) andMaxAge = sub.getMaxAge();
                            break;
                    }
                }
                if (andMinTickets != null && andMaxTickets != null && andMinTickets > andMaxTickets) {
                    throw new IllegalArgumentException(
                            "In AND rule: minTickets (" + andMinTickets + ") cannot be greater than maxTickets (" + andMaxTickets + ")");
                }
                if (andMinAge != null && andMaxAge != null && andMinAge > andMaxAge) {
                    throw new IllegalArgumentException(
                            "In AND rule: minAge (" + andMinAge + ") cannot be greater than maxAge (" + andMaxAge + ")");
                }
                List<ITicketPurchaseRule> andAdapters = new ArrayList<>();
                List<IPurchaseRule> andTeammateRules = new ArrayList<>();
                for (PolicyRequest sub : req.getSubPolicies()) {
                    ITicketPurchaseRule subAdapter = buildRule(sub);
                    if (subAdapter instanceof PurchaseRuleAdapter) {
                        andAdapters.add(subAdapter);
                        andTeammateRules.add(((PurchaseRuleAdapter) subAdapter).getTargetRule());
                    }
                }
                IPurchaseRule andRule = new AndRule(andTeammateRules.toArray(new IPurchaseRule[0]));
                return new PurchaseRuleAdapter(andRule, "AND", null, null, null, null, andAdapters);
            case "OR":
                if (req.getSubPolicies() == null) {
                    throw new IllegalArgumentException("subPolicies are required for OR composition");
                }
                List<ITicketPurchaseRule> orAdapters = new ArrayList<>();
                List<IPurchaseRule> orTeammateRules = new ArrayList<>();
                for (PolicyRequest sub : req.getSubPolicies()) {
                    ITicketPurchaseRule subAdapter = buildRule(sub);
                    if (subAdapter instanceof PurchaseRuleAdapter) {
                        orAdapters.add(subAdapter);
                        orTeammateRules.add(((PurchaseRuleAdapter) subAdapter).getTargetRule());
                    }
                }
                IPurchaseRule orRule = new OrRule(orTeammateRules.toArray(new IPurchaseRule[0]));
                return new PurchaseRuleAdapter(orRule, "OR", null, null, null, null, orAdapters);
            default:
                throw new IllegalArgumentException("Unknown policy type: " + req.getType());
        }
    }

    private String describeRule(ITicketPurchaseRule rule) {
        if (rule == null) {
            return "No policy";
        }
        if (rule instanceof PurchaseRuleAdapter) {
            PurchaseRuleAdapter adapter = (PurchaseRuleAdapter) rule;
            switch (adapter.getType().toUpperCase()) {
                case "AGE":
                    return "Age limit: Min=" + adapter.getMinAge() + ", Max=" + adapter.getMaxAge();
                case "MIN_TICKETS":
                    return "Min tickets: " + adapter.getMinTickets();
                case "MAX_TICKETS":
                    return "Max tickets: " + adapter.getMaxTickets();
                case "AND":
                    List<String> andDescriptions = new ArrayList<>();
                    for (ITicketPurchaseRule sub : adapter.getSubRules()) {
                        andDescriptions.add(describeRule(sub));
                    }
                    return "AND(" + String.join(", ", andDescriptions) + ")";
                case "OR":
                    List<String> orDescriptions = new ArrayList<>();
                    for (ITicketPurchaseRule sub : adapter.getSubRules()) {
                        orDescriptions.add(describeRule(sub));
                    }
                    return "OR(" + String.join(", ", orDescriptions) + ")";
            }
        }
        return rule.getClass().getSimpleName();
    }

    // DTO Classes
    public static class ValidationRequest {
        private String eventId;
        private int buyerAge;
        private int requestedTickets;

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public int getBuyerAge() { return buyerAge; }
        public void setBuyerAge(int buyerAge) { this.buyerAge = buyerAge; }

        public int getRequestedTickets() { return requestedTickets; }
        public void setRequestedTickets(int requestedTickets) { this.requestedTickets = requestedTickets; }
    }

    public static class PolicyRequest {
        private String type;
        private Integer minAge;
        private Integer maxAge;
        private Integer minTickets;
        private Integer maxTickets;
        private List<PolicyRequest> subPolicies;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Integer getMinAge() { return minAge; }
        public void setMinAge(Integer minAge) { this.minAge = minAge; }

        public Integer getMaxAge() { return maxAge; }
        public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

        public Integer getMinTickets() { return minTickets; }
        public void setMinTickets(Integer minTickets) { this.minTickets = minTickets; }

        public Integer getMaxTickets() { return maxTickets; }
        public void setMaxTickets(Integer maxTickets) { this.maxTickets = maxTickets; }

        public List<PolicyRequest> getSubPolicies() { return subPolicies; }
        public void setSubPolicies(List<PolicyRequest> subPolicies) { this.subPolicies = subPolicies; }
    }

    public static class PolicyResponse {
        private String description;

        public PolicyResponse(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
