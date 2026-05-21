package com.ticketpurchasingsystem.project.acceptance.production;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchaseContext;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.AndRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MaxTicketsRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinTicketsRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.OrRule;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProductionPurchasePolicyAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";

    private AuthenticationService authService;
    private ProdRepo prodRepo;
    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);
        prodRepo = new ProdRepo();
        ProductionEventPublisher publisher = new ProductionEventPublisher(event -> {
        });
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo, publisher);
    }

    private Integer createCompany(String token, String name) {
        productionService.createProductionCompany(token, new ProductionCompanyDTO(name, "desc", "test@company.com"));
        return prodRepo.findByName(name).get().getCompanyId();
    }

    @Test
    void GivenOwner_WhenAddMinAgeRule_ThenPolicyBlocksUnderage() {
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "AgePolicy Corp");

        boolean added = productionService.addPurchasePolicyRule(token, companyId, new MinAgeRule(18));

        assertTrue(added);
        ProductionCompany company = prodRepo.findById(companyId).get();
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(1, 17)));
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(1, 18)));
    }

    @Test
    void GivenOwner_WhenAddMaxTicketsRule_ThenPolicyEnforcesLimit() {
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "Tickets Corp");

        productionService.addPurchasePolicyRule(token, companyId, new MaxTicketsRule(5));

        ProductionCompany company = prodRepo.findById(companyId).get();
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(6, 25)));
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(5, 25)));
    }

    @Test
    void GivenOwner_WhenAddAndRule_ThenPolicyEnforcesAllConditions() {
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "AndRule Corp");

        IPurchaseRule rule = new AndRule(new MinTicketsRule(2), new MaxTicketsRule(5));
        productionService.addPurchasePolicyRule(token, companyId, rule);

        ProductionCompany company = prodRepo.findById(companyId).get();
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(1, 25))); // below min
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(6, 25))); // above max
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(3, 25))); // in range
    }

    @Test
    void GivenOwner_WhenAddOrRule_ThenPolicyAllowsEitherCondition() {
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "OrRule Corp");

        IPurchaseRule rule = new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100));
        productionService.addPurchasePolicyRule(token, companyId, rule);

        ProductionCompany company = prodRepo.findById(companyId).get();
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(1, 25))); // <= 2
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(100, 25))); // >= 100
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(50, 25))); // neither
    }

    @Test
    void GivenOwner_WhenAddNestedCompositeRule_ThenPolicyEvaluatesDepthCorrectly() {
        // "From age 18 AND (up to 2 tickets OR at least 100 tickets)"
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "Nested Corp");

        IPurchaseRule rule = new AndRule(
                new MinAgeRule(18),
                new OrRule(new MaxTicketsRule(2), new MinTicketsRule(100)));
        productionService.addPurchasePolicyRule(token, companyId, rule);

        ProductionCompany company = prodRepo.findById(companyId).get();
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(1, 20))); // adult, <=2
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(100, 20))); // adult, >=100
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(50, 20))); // adult, mid amount
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(1, 16))); // underage
    }

    @Test
    void GivenOwner_WhenAddMultipleRules_ThenAllRulesAreEnforced() {
        String token = authService.login("owner");
        Integer companyId = createCompany(token, "Multi Corp");

        productionService.addPurchasePolicyRule(token, companyId, new MinAgeRule(18));
        productionService.addPurchasePolicyRule(token, companyId, new MaxTicketsRule(5));

        ProductionCompany company = prodRepo.findById(companyId).get();
        assertTrue(company.getPurchasePolicy().validate(new PurchaseContext(3, 20))); // both pass
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(3, 16))); // age fails
        assertFalse(company.getPurchasePolicy().validate(new PurchaseContext(6, 20))); // tickets fails
    }

    @Test
    void GivenNonOwner_WhenAddPurchasePolicyRule_ThenReturnFalse() {
        String ownerToken = authService.login("owner");
        Integer companyId = createCompany(ownerToken, "Security Corp");

        String nonOwnerToken = authService.login("nonOwner");
        boolean result = productionService.addPurchasePolicyRule(nonOwnerToken, companyId, new MinAgeRule(18));

        assertFalse(result);
    }

    @Test
    void GivenInvalidToken_WhenAddPurchasePolicyRule_ThenReturnFalse() {
        String ownerToken = authService.login("owner");
        Integer companyId = createCompany(ownerToken, "Token Corp");

        boolean result = productionService.addPurchasePolicyRule("invalid-token", companyId, new MinAgeRule(18));

        assertFalse(result);
    }

    @Test
    void GivenOwner_WhenAddRuleToNonExistentCompany_ThenReturnFalse() {
        String token = authService.login("owner");

        boolean result = productionService.addPurchasePolicyRule(token, 99999, new MinAgeRule(18));

        assertFalse(result);
    }
}
