package com.ticketpurchasingsystem.project.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.event.Event;
import com.ticketpurchasingsystem.project.domain.event.EventDiscountPolicy;
import com.ticketpurchasingsystem.project.domain.event.Purchase_Policy.EventPurchasePolicy;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import com.ticketpurchasingsystem.project.domain.tickets.*;
import com.ticketpurchasingsystem.project.infrastructure.PurchasePolicyController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PurchasePolicyTest {

    @Mock
    private IEventRepo eventRepo;

    @Mock
    private IProdRepo prodRepo;

    private PurchasePolicyService purchasePolicyService;
    private PurchasePolicyController purchasePolicyController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        purchasePolicyService = new PurchasePolicyService(eventRepo, prodRepo);
        purchasePolicyController = new PurchasePolicyController(purchasePolicyService);
        mockMvc = MockMvcBuilders.standaloneSetup(purchasePolicyController).build();
        objectMapper = new ObjectMapper();
    }

    // ─── Phase 2 & 3: Domain Rules & Composites Tests ──────────────────────────────────────────

    @Test
    void testAgePurchasePolicy() {
        // Age policy: min 18, max 60
        ITicketPurchaseRule rule = new AgePurchasePolicy(18, 60);

        assertTrue(rule.validate(new TicketPurchaseContext(25, 2)).isValid());
        assertTrue(rule.validate(new TicketPurchaseContext(18, 2)).isValid());
        assertTrue(rule.validate(new TicketPurchaseContext(60, 2)).isValid());

        PolicyValidationResult tooYoung = rule.validate(new TicketPurchaseContext(17, 2));
        assertFalse(tooYoung.isValid());
        assertTrue(tooYoung.getRejectionMessage().contains("less than the minimum required age"));

        PolicyValidationResult tooOld = rule.validate(new TicketPurchaseContext(61, 2));
        assertFalse(tooOld.isValid());
        assertTrue(tooOld.getRejectionMessage().contains("exceeds the maximum allowed age"));

        // Age policy with null bounds
        ITicketPurchaseRule onlyMinAge = new AgePurchasePolicy(18, null);
        assertTrue(onlyMinAge.validate(new TicketPurchaseContext(100, 2)).isValid());
        assertFalse(onlyMinAge.validate(new TicketPurchaseContext(16, 2)).isValid());

        ITicketPurchaseRule onlyMaxAge = new AgePurchasePolicy(null, 50);
        assertTrue(onlyMaxAge.validate(new TicketPurchaseContext(5, 2)).isValid());
        assertFalse(onlyMaxAge.validate(new TicketPurchaseContext(55, 2)).isValid());
    }

    @Test
    void testMinTicketsPurchasePolicy() {
        ITicketPurchaseRule rule = new MinTicketsPurchasePolicy(3);

        assertTrue(rule.validate(new TicketPurchaseContext(20, 3)).isValid());
        assertTrue(rule.validate(new TicketPurchaseContext(20, 5)).isValid());

        PolicyValidationResult fail = rule.validate(new TicketPurchaseContext(20, 2));
        assertFalse(fail.isValid());
        assertTrue(fail.getRejectionMessage().contains("less than the minimum limit"));
    }

    @Test
    void testMaxTicketsPurchasePolicy() {
        ITicketPurchaseRule rule = new MaxTicketsPurchasePolicy(6);

        assertTrue(rule.validate(new TicketPurchaseContext(20, 6)).isValid());
        assertTrue(rule.validate(new TicketPurchaseContext(20, 4)).isValid());

        PolicyValidationResult fail = rule.validate(new TicketPurchaseContext(20, 7));
        assertFalse(fail.isValid());
        assertTrue(fail.getRejectionMessage().contains("exceeds the maximum limit"));
    }

    @Test
    void testAndPolicyComposition() {
        ITicketPurchaseRule rule1 = new AgePurchasePolicy(18, null);
        ITicketPurchaseRule rule2 = new MaxTicketsPurchasePolicy(4);
        ITicketPurchaseRule andRule = new AndPolicyComposition(Arrays.asList(rule1, rule2));

        // Both pass
        assertTrue(andRule.validate(new TicketPurchaseContext(20, 3)).isValid());

        // First fails
        PolicyValidationResult fail1 = andRule.validate(new TicketPurchaseContext(16, 3));
        assertFalse(fail1.isValid());
        assertTrue(fail1.getRejectionMessage().contains("minimum required age"));

        // Second fails
        PolicyValidationResult fail2 = andRule.validate(new TicketPurchaseContext(20, 5));
        assertFalse(fail2.isValid());
        assertTrue(fail2.getRejectionMessage().contains("maximum limit"));
    }

    @Test
    void testOrPolicyComposition() {
        ITicketPurchaseRule rule1 = new AgePurchasePolicy(18, null);
        ITicketPurchaseRule rule2 = new MaxTicketsPurchasePolicy(2);
        ITicketPurchaseRule orRule = new OrPolicyComposition(Arrays.asList(rule1, rule2));

        // Both pass
        assertTrue(orRule.validate(new TicketPurchaseContext(20, 1)).isValid());

        // Rule 1 passes, Rule 2 fails (buyer age >= 18 but ticket count > 2)
        assertTrue(orRule.validate(new TicketPurchaseContext(20, 5)).isValid());

        // Rule 1 fails, Rule 2 passes (buyer age < 18 but ticket count <= 2)
        assertTrue(orRule.validate(new TicketPurchaseContext(15, 2)).isValid());

        // Both fail
        PolicyValidationResult bothFail = orRule.validate(new TicketPurchaseContext(15, 5));
        assertFalse(bothFail.isValid());
        // Should combine failure messages with " OR "
        String expectedMessage = "Buyer age 15 is less than the minimum required age of 18. OR Requested tickets 5 exceeds the maximum limit of 2.";
        assertEquals(expectedMessage, bothFail.getRejectionMessage());
    }

    // ─── Phase 4: Service Assignment & Integration Tests ──────────────────────────────────────

    @Test
    void testValidatePurchaseSuccessWhenNoPolicies() {
        Event event = createMockEvent();
        when(eventRepo.findById("123")).thenReturn(event);
        when(prodRepo.findById(1)).thenReturn(Optional.empty());

        PolicyValidationResult result = purchasePolicyService.validatePurchase("123", 25, 4);
        assertTrue(result.isValid());
    }

    @Test
    void testValidatePurchaseFailsOnEventPolicy() {
        Event event = createMockEvent();
        event.setTicketPurchasePolicy(new AgePurchasePolicy(18, null));
        when(eventRepo.findById("123")).thenReturn(event);

        PolicyValidationResult result = purchasePolicyService.validatePurchase("123", 16, 2);
        assertFalse(result.isValid());
        assertTrue(result.getRejectionMessage().contains("minimum required age"));
    }

    @Test
    void testValidatePurchaseFailsOnCompanyPolicy() {
        Event event = createMockEvent();
        event.setTicketPurchasePolicy(null); // Event policy passes

        ProductionCompany company = createMockCompany();
        company.setTicketPurchasePolicy(new MaxTicketsPurchasePolicy(3)); // Company policy fails for 4 tickets

        when(eventRepo.findById("123")).thenReturn(event);
        when(prodRepo.findById(1)).thenReturn(Optional.of(company));

        PolicyValidationResult result = purchasePolicyService.validatePurchase("123", 25, 4);
        assertFalse(result.isValid());
        assertTrue(result.getRejectionMessage().contains("maximum limit"));
    }

    @Test
    void testAssignAndGetPolicies() {
        Event event = createMockEvent();
        when(eventRepo.findById("123")).thenReturn(event);

        ITicketPurchaseRule rule = new MinTicketsPurchasePolicy(2);
        purchasePolicyService.assignPolicyToEvent("123", rule);

        verify(eventRepo, times(1)).save(event);
        assertEquals(rule, purchasePolicyService.getPolicyByEvent("123"));

        ProductionCompany company = createMockCompany();
        when(prodRepo.findById(1)).thenReturn(Optional.of(company));

        purchasePolicyService.assignPolicyToCompany(1, rule);
        verify(prodRepo, times(1)).save(company);
        assertEquals(rule, purchasePolicyService.getPolicyByCompany(1));
    }

    // ─── Phase 5: Controller Exposure Tests ──────────────────────────────────────────────────

    @Test
    void testControllerValidatePurchaseEndpoint() throws Exception {
        Event event = createMockEvent();
        event.setTicketPurchasePolicy(new MinTicketsPurchasePolicy(3));
        when(eventRepo.findById("123")).thenReturn(event);

        PurchasePolicyController.ValidationRequest request = new PurchasePolicyController.ValidationRequest();
        request.setEventId("123");
        request.setBuyerAge(25);
        request.setRequestedTickets(2); // Should fail min limit of 3

        mockMvc.perform(post("/api/policies/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.rejectionMessage").value("Requested tickets 2 is less than the minimum limit of 3."));
    }

    @Test
    void testControllerAssignAndGetEndpoints() throws Exception {
        Event event = createMockEvent();
        when(eventRepo.findById("123")).thenReturn(event);

        // Assign Age Policy to Event via POST
        PurchasePolicyController.PolicyRequest request = new PurchasePolicyController.PolicyRequest();
        request.setType("AGE");
        request.setMinAge(18);

        mockMvc.perform(post("/api/policies/assign/event/123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify rule is assigned
        assertNotNull(event.getTicketPurchasePolicy());
        assertTrue(event.getTicketPurchasePolicy() instanceof AgePurchasePolicy);

        // Retrieve assigned policy via GET
        mockMvc.perform(get("/api/policies/event/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Age limit: Min=18, Max=null"));
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────────────────────

    private Event createMockEvent() {
        EventPurchasePolicy purchasePolicy = new EventPurchasePolicy();
        EventDiscountPolicy discountPolicy = new EventDiscountPolicy(new ArrayList<>());
        Event event = new Event(
                1,
                "Rock Concert",
                100,
                LocalDateTime.now().plusDays(2),
                purchasePolicy,
                discountPolicy,
                0
        );
        event.setEventId("123");
        return event;
    }

    private ProductionCompany createMockCompany() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO();
        dto.setCompanyName("Main Stage Productions");
        dto.setCompanyEmail("info@mainstage.com");
        dto.setCompanyDescription("Main Stage Productions company profile");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(1);
        return company;
    }
}
