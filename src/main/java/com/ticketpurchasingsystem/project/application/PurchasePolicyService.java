package com.ticketpurchasingsystem.project.application;

import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.tickets.ITicketPurchaseRule;
import com.ticketpurchasingsystem.project.domain.tickets.PolicyValidationResult;
import com.ticketpurchasingsystem.project.domain.tickets.TicketPurchaseContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PurchasePolicyService {

    private final IEventRepo eventRepo;
    private final IProdRepo prodRepo;

    public PurchasePolicyService(IEventRepo eventRepo, IProdRepo prodRepo) {
        this.eventRepo = eventRepo;
        this.prodRepo = prodRepo;
    }

    public void assignPolicyToEvent(String eventId, ITicketPurchaseRule policy) {
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found with ID: " + eventId);
        }
        event.setTicketPurchasePolicy(policy);
        eventRepo.save(event);
    }

    public void assignPolicyToCompany(Integer companyId, ITicketPurchaseRule policy) {
        Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Production company not found with ID: " + companyId);
        }
        ProductionCompany company = companyOpt.get();
        company.setTicketPurchasePolicy(policy);
        prodRepo.save(company);
    }

    public ITicketPurchaseRule getPolicyByEvent(String eventId) {
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event not found with ID: " + eventId);
        }
        return event.getTicketPurchasePolicy();
    }

    public ITicketPurchaseRule getPolicyByCompany(Integer companyId) {
        Optional<ProductionCompany> companyOpt = prodRepo.findById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Production company not found with ID: " + companyId);
        }
        return companyOpt.get().getTicketPurchasePolicy();
    }

    public PolicyValidationResult validatePurchase(String eventId, int buyerAge, int requestedTickets) {
        Event event = eventRepo.findById(eventId);
        if (event == null) {
            return PolicyValidationResult.fail("Event not found");
        }

        TicketPurchaseContext context = new TicketPurchaseContext(buyerAge, requestedTickets);

        // Validate against Event Policy
        ITicketPurchaseRule eventPolicy = event.getTicketPurchasePolicy();
        if (eventPolicy != null) {
            PolicyValidationResult result = eventPolicy.validate(context);
            if (!result.isValid()) {
                return result;
            }
        }

        // Validate against Production Company Policy
        Optional<ProductionCompany> companyOpt = prodRepo.findById(event.getCompanyId());
        if (companyOpt.isPresent()) {
            ITicketPurchaseRule companyPolicy = companyOpt.get().getTicketPurchasePolicy();
            if (companyPolicy != null) {
                PolicyValidationResult result = companyPolicy.validate(context);
                if (!result.isValid()) {
                    return result;
                }
            }
        }

        return PolicyValidationResult.success();
    }
}
