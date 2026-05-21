package com.ticketpurchasingsystem.project.infrastructure;

import com.ticketpurchasingsystem.project.application.PurchasePolicyService;
import com.ticketpurchasingsystem.project.domain.tickets.*;
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
                return new AgePurchasePolicy(req.getMinAge(), req.getMaxAge());
            case "MIN_TICKETS":
                if (req.getMinTickets() == null) {
                    throw new IllegalArgumentException("minTickets is required for MIN_TICKETS policy");
                }
                return new MinTicketsPurchasePolicy(req.getMinTickets());
            case "MAX_TICKETS":
                if (req.getMaxTickets() == null) {
                    throw new IllegalArgumentException("maxTickets is required for MAX_TICKETS policy");
                }
                return new MaxTicketsPurchasePolicy(req.getMaxTickets());
            case "AND":
                if (req.getSubPolicies() == null) {
                    throw new IllegalArgumentException("subPolicies are required for AND composition");
                }
                List<ITicketPurchaseRule> andRules = new ArrayList<>();
                for (PolicyRequest sub : req.getSubPolicies()) {
                    andRules.add(buildRule(sub));
                }
                return new AndPolicyComposition(andRules);
            case "OR":
                if (req.getSubPolicies() == null) {
                    throw new IllegalArgumentException("subPolicies are required for OR composition");
                }
                List<ITicketPurchaseRule> orRules = new ArrayList<>();
                for (PolicyRequest sub : req.getSubPolicies()) {
                    orRules.add(buildRule(sub));
                }
                return new OrPolicyComposition(orRules);
            default:
                throw new IllegalArgumentException("Unknown policy type: " + req.getType());
        }
    }

    private String describeRule(ITicketPurchaseRule rule) {
        if (rule == null) {
            return "No policy";
        }
        if (rule instanceof AgePurchasePolicy) {
            AgePurchasePolicy age = (AgePurchasePolicy) rule;
            return "Age limit: Min=" + age.getMinAge() + ", Max=" + age.getMaxAge();
        } else if (rule instanceof MinTicketsPurchasePolicy) {
            MinTicketsPurchasePolicy min = (MinTicketsPurchasePolicy) rule;
            return "Min tickets: " + min.getMinTickets();
        } else if (rule instanceof MaxTicketsPurchasePolicy) {
            MaxTicketsPurchasePolicy max = (MaxTicketsPurchasePolicy) rule;
            return "Max tickets: " + max.getMaxTickets();
        } else if (rule instanceof AndPolicyComposition) {
            AndPolicyComposition and = (AndPolicyComposition) rule;
            List<String> subDescriptions = new ArrayList<>();
            for (ITicketPurchaseRule sub : and.getRules()) {
                subDescriptions.add(describeRule(sub));
            }
            return "AND(" + String.join(", ", subDescriptions) + ")";
        } else if (rule instanceof OrPolicyComposition) {
            OrPolicyComposition or = (OrPolicyComposition) rule;
            List<String> subDescriptions = new ArrayList<>();
            for (ITicketPurchaseRule sub : or.getRules()) {
                subDescriptions.add(describeRule(sub));
            }
            return "OR(" + String.join(", ", subDescriptions) + ")";
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
