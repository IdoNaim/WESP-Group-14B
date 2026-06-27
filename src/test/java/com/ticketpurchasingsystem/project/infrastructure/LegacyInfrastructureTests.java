package com.ticketpurchasingsystem.project.infrastructure;

import com.ticketpurchasingsystem.project.Controllers.PurchasePolicyController;
import com.ticketpurchasingsystem.project.application.PaymentDetails;
import com.ticketpurchasingsystem.project.application.PurchasePolicyService;
import org.springframework.web.client.RestTemplate;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.BarcodeDTO;
import com.ticketpurchasingsystem.project.domain.tickets.ITicketPurchaseRule;
import com.ticketpurchasingsystem.project.domain.tickets.PolicyValidationResult;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.tickets.PurchaseRuleAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LegacyInfrastructureTests {

    @Test
    public void GivenValidPaymentDetails_WhenPayRefundAndHandshake_ThenPaymentGatewayReturnsExpectedResults() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn("50000")  // pay
                .thenReturn("1")      // refund
                .thenReturn("OK");    // handshake

        PaymentGateway gateway = new PaymentGateway(restTemplate);
        PaymentDetails details = new PaymentDetails(100.0, "USD", "4111111111111111",
                "12", "2028", "John Doe", "123", "ID-001");

        int txId = gateway.pay(details);
        assertTrue(txId >= 10000 && txId <= 100000);
        assertEquals(1, gateway.refund(txId));
        assertTrue(gateway.handshake());
    }
    @Test
    public void GivenValidOrderWithSeatsAndStandingAreas_WhenIssueBarcodes_ThenReturnBarcodeForEachTicket() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn("TIX-AAA-0001")
                .thenReturn("TIX-BBB-0002")
                .thenReturn("TIX-CCC-0003")
                .thenReturn("TIX-DDD-0004")
                .thenReturn("TIX-EEE-0005");

        BarCodeGateway gateway = new BarCodeGateway(restTemplate);

        // seat IDs must follow zone_row_seat format used by AssignedSeat
        List<String> seatIds = List.of("0_1_1", "0_1_2");
        HashMap<String, Integer> standingQuantities = new HashMap<>();
        standingQuantities.put("1", 3);

        ActiveOrderDTO order = new ActiveOrderDTO("order1", "user1", "event1",
                new Timestamp(System.currentTimeMillis()), seatIds, standingQuantities);

        List<BarcodeDTO> barcodes = gateway.issueBarcodes(order);
        assertEquals(5, barcodes.size());
        assertEquals("TIX-AAA-0001", barcodes.get(0).getBarcodeValue());
        assertEquals("TIX-BBB-0002", barcodes.get(1).getBarcodeValue());
        assertEquals("TIX-CCC-0003", barcodes.get(2).getBarcodeValue());
        assertEquals("TIX-DDD-0004", barcodes.get(3).getBarcodeValue());
        assertEquals("TIX-EEE-0005", barcodes.get(4).getBarcodeValue());
    }
    @Test
    public void GivenValidPolicyRequest_WhenValidatePurchase_ThenReturnSuccessResponse() {
        PurchasePolicyService mockService = mock(PurchasePolicyService.class);
        PolicyValidationResult result = PolicyValidationResult.success();
        when(mockService.validatePurchase("event1", 25, 2)).thenReturn(result);

        PurchasePolicyController controller = new PurchasePolicyController(mockService);
        PurchasePolicyController.ValidationRequest request = new PurchasePolicyController.ValidationRequest();
        request.setEventId("event1");
        request.setBuyerAge(25);
        request.setRequestedTickets(2);

        assertEquals("event1", request.getEventId());
        assertEquals(25, request.getBuyerAge());
        assertEquals(2, request.getRequestedTickets());

        ResponseEntity<PolicyValidationResult> response = controller.validatePurchase(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isValid());
    }

    @Test
    public void GivenVariousPolicyTypes_WhenAssignPolicyToEvent_ThenReturnExpectedHttpStatus() {
        PurchasePolicyService mockService = mock(PurchasePolicyService.class);
        PurchasePolicyController controller = new PurchasePolicyController(mockService);

        // 1. null request
        ResponseEntity<String> resNull = controller.assignPolicyToEvent("event1", null);
        assertEquals(HttpStatus.OK, resNull.getStatusCode());

        // 2. null type
        PurchasePolicyController.PolicyRequest reqNullType = new PurchasePolicyController.PolicyRequest();
        ResponseEntity<String> resNullType = controller.assignPolicyToEvent("event1", reqNullType);
        assertEquals(HttpStatus.OK, resNullType.getStatusCode());

        // 3. Unknown policy type
        PurchasePolicyController.PolicyRequest reqUnknown = new PurchasePolicyController.PolicyRequest();
        reqUnknown.setType("UNKNOWN");
        ResponseEntity<String> resUnknown = controller.assignPolicyToEvent("event1", reqUnknown);
        assertEquals(HttpStatus.BAD_REQUEST, resUnknown.getStatusCode());
        assertTrue(resUnknown.getBody().contains("Unknown policy type"));

        // 4. AGE - both min and max
        PurchasePolicyController.PolicyRequest reqAgeBoth = new PurchasePolicyController.PolicyRequest();
        reqAgeBoth.setType("AGE");
        reqAgeBoth.setMinAge(18);
        reqAgeBoth.setMaxAge(60);
        ResponseEntity<String> resAgeBoth = controller.assignPolicyToEvent("event1", reqAgeBoth);
        assertEquals(HttpStatus.OK, resAgeBoth.getStatusCode());

        // 5. AGE - min only
        PurchasePolicyController.PolicyRequest reqAgeMin = new PurchasePolicyController.PolicyRequest();
        reqAgeMin.setType("AGE");
        reqAgeMin.setMinAge(18);
        ResponseEntity<String> resAgeMin = controller.assignPolicyToEvent("event1", reqAgeMin);
        assertEquals(HttpStatus.OK, resAgeMin.getStatusCode());

        // 6. AGE - max only
        PurchasePolicyController.PolicyRequest reqAgeMax = new PurchasePolicyController.PolicyRequest();
        reqAgeMax.setType("AGE");
        reqAgeMax.setMaxAge(60);
        ResponseEntity<String> resAgeMax = controller.assignPolicyToEvent("event1", reqAgeMax);
        assertEquals(HttpStatus.OK, resAgeMax.getStatusCode());

        // 7. AGE - none (exception)
        PurchasePolicyController.PolicyRequest reqAgeNone = new PurchasePolicyController.PolicyRequest();
        reqAgeNone.setType("AGE");
        ResponseEntity<String> resAgeNone = controller.assignPolicyToEvent("event1", reqAgeNone);
        assertEquals(HttpStatus.BAD_REQUEST, resAgeNone.getStatusCode());

        // 8. MIN_TICKETS
        PurchasePolicyController.PolicyRequest reqMinTickets = new PurchasePolicyController.PolicyRequest();
        reqMinTickets.setType("MIN_TICKETS");
        reqMinTickets.setMinTickets(2);
        ResponseEntity<String> resMinTickets = controller.assignPolicyToEvent("event1", reqMinTickets);
        assertEquals(HttpStatus.OK, resMinTickets.getStatusCode());

        // 9. MIN_TICKETS - null
        PurchasePolicyController.PolicyRequest reqMinTicketsNull = new PurchasePolicyController.PolicyRequest();
        reqMinTicketsNull.setType("MIN_TICKETS");
        ResponseEntity<String> resMinTicketsNull = controller.assignPolicyToEvent("event1", reqMinTicketsNull);
        assertEquals(HttpStatus.BAD_REQUEST, resMinTicketsNull.getStatusCode());

        // 10. MAX_TICKETS
        PurchasePolicyController.PolicyRequest reqMaxTickets = new PurchasePolicyController.PolicyRequest();
        reqMaxTickets.setType("MAX_TICKETS");
        reqMaxTickets.setMaxTickets(10);
        ResponseEntity<String> resMaxTickets = controller.assignPolicyToEvent("event1", reqMaxTickets);
        assertEquals(HttpStatus.OK, resMaxTickets.getStatusCode());

        // 11. MAX_TICKETS - null
        PurchasePolicyController.PolicyRequest reqMaxTicketsNull = new PurchasePolicyController.PolicyRequest();
        reqMaxTicketsNull.setType("MAX_TICKETS");
        ResponseEntity<String> resMaxTicketsNull = controller.assignPolicyToEvent("event1", reqMaxTicketsNull);
        assertEquals(HttpStatus.BAD_REQUEST, resMaxTicketsNull.getStatusCode());

        // 12. AND
        PurchasePolicyController.PolicyRequest reqAnd = new PurchasePolicyController.PolicyRequest();
        reqAnd.setType("AND");
        reqAnd.setSubPolicies(List.of(reqAgeMin, reqAgeMax));
        ResponseEntity<String> resAnd = controller.assignPolicyToEvent("event1", reqAnd);
        assertEquals(HttpStatus.OK, resAnd.getStatusCode());

        // 13. AND - null sub
        PurchasePolicyController.PolicyRequest reqAndNull = new PurchasePolicyController.PolicyRequest();
        reqAndNull.setType("AND");
        ResponseEntity<String> resAndNull = controller.assignPolicyToEvent("event1", reqAndNull);
        assertEquals(HttpStatus.BAD_REQUEST, resAndNull.getStatusCode());

        // 14. OR
        PurchasePolicyController.PolicyRequest reqOr = new PurchasePolicyController.PolicyRequest();
        reqOr.setType("OR");
        reqOr.setSubPolicies(List.of(reqAgeMin, reqAgeMax));
        ResponseEntity<String> resOr = controller.assignPolicyToEvent("event1", reqOr);
        assertEquals(HttpStatus.OK, resOr.getStatusCode());

        // 15. OR - null sub
        PurchasePolicyController.PolicyRequest reqOrNull = new PurchasePolicyController.PolicyRequest();
        reqOrNull.setType("OR");
        ResponseEntity<String> resOrNull = controller.assignPolicyToEvent("event1", reqOrNull);
        assertEquals(HttpStatus.BAD_REQUEST, resOrNull.getStatusCode());

        // 16. Company assignment
        ResponseEntity<String> resCompany = controller.assignPolicyToCompany(1, reqAgeMin);
        assertEquals(HttpStatus.OK, resCompany.getStatusCode());

        // 17. Company assignment failure
        doThrow(new IllegalArgumentException("Company not found")).when(mockService).assignPolicyToCompany(anyInt(), any());
        ResponseEntity<String> resCompanyFail = controller.assignPolicyToCompany(999, reqAgeMin);
        assertEquals(HttpStatus.BAD_REQUEST, resCompanyFail.getStatusCode());
    }

    @Test
    public void GivenVariousPolicyAdapters_WhenGetPolicyByEvent_ThenReturnCorrectDescription() {
        PurchasePolicyService mockService = mock(PurchasePolicyService.class);
        PurchasePolicyController controller = new PurchasePolicyController(mockService);

        // No policy
        when(mockService.getPolicyByEvent("event1")).thenReturn(null);
        ResponseEntity<PurchasePolicyController.PolicyResponse> resNull = controller.getPolicyByEvent("event1");
        assertEquals(HttpStatus.OK, resNull.getStatusCode());
        assertEquals("No policy", resNull.getBody().getDescription());

        // Exception path
        when(mockService.getPolicyByEvent("event2")).thenThrow(new IllegalArgumentException("Not found"));
        ResponseEntity<PurchasePolicyController.PolicyResponse> resEx = controller.getPolicyByEvent("event2");
        assertEquals(HttpStatus.NOT_FOUND, resEx.getStatusCode());

        // AGE adapter
        PurchaseRuleAdapter ageAdapter = new PurchaseRuleAdapter(new MinAgeRule(18), "AGE", 18, 60, null, null, null);
        when(mockService.getPolicyByEvent("event_age")).thenReturn(ageAdapter);
        ResponseEntity<PurchasePolicyController.PolicyResponse> resAge = controller.getPolicyByEvent("event_age");
        assertEquals("Age limit: Min=18, Max=60", resAge.getBody().getDescription());

        // MIN_TICKETS adapter
        PurchaseRuleAdapter minAdapter = new PurchaseRuleAdapter(new MinAgeRule(18), "MIN_TICKETS", null, null, 2, null, null);
        when(mockService.getPolicyByEvent("event_min")).thenReturn(minAdapter);
        ResponseEntity<PurchasePolicyController.PolicyResponse> resMin = controller.getPolicyByEvent("event_min");
        assertEquals("Min tickets: 2", resMin.getBody().getDescription());

        // MAX_TICKETS adapter
        PurchaseRuleAdapter maxAdapter = new PurchaseRuleAdapter(new MinAgeRule(18), "MAX_TICKETS", null, null, null, 10, null);
        when(mockService.getPolicyByEvent("event_max")).thenReturn(maxAdapter);
        ResponseEntity<PurchasePolicyController.PolicyResponse> resMax = controller.getPolicyByEvent("event_max");
        assertEquals("Max tickets: 10", resMax.getBody().getDescription());

        // AND adapter
        PurchaseRuleAdapter andAdapter = new PurchaseRuleAdapter(new MinAgeRule(18), "AND", null, null, null, null, List.of(ageAdapter, minAdapter));
        when(mockService.getPolicyByEvent("event_and")).thenReturn(andAdapter);
        ResponseEntity<PurchasePolicyController.PolicyResponse> resAnd = controller.getPolicyByEvent("event_and");
        assertEquals("AND(Age limit: Min=18, Max=60, Min tickets: 2)", resAnd.getBody().getDescription());

        // OR adapter
        PurchaseRuleAdapter orAdapter = new PurchaseRuleAdapter(new MinAgeRule(18), "OR", null, null, null, null, List.of(ageAdapter, minAdapter));
        when(mockService.getPolicyByEvent("event_or")).thenReturn(orAdapter);
        ResponseEntity<PurchasePolicyController.PolicyResponse> resOr = controller.getPolicyByEvent("event_or");
        assertEquals("OR(Age limit: Min=18, Max=60, Min tickets: 2)", resOr.getBody().getDescription());

        // Other rule type fallback
        ITicketPurchaseRule customRule = mock(ITicketPurchaseRule.class);
        when(mockService.getPolicyByEvent("event_custom")).thenReturn(customRule);
        ResponseEntity<PurchasePolicyController.PolicyResponse> resCustom = controller.getPolicyByEvent("event_custom");
        assertTrue(resCustom.getBody().getDescription().contains("MockitoMock"));

        // Company test
        when(mockService.getPolicyByCompany(1)).thenReturn(ageAdapter);
        ResponseEntity<PurchasePolicyController.PolicyResponse> resCompany = controller.getPolicyByCompany(1);
        assertEquals(HttpStatus.OK, resCompany.getStatusCode());
        assertEquals("Age limit: Min=18, Max=60", resCompany.getBody().getDescription());

        when(mockService.getPolicyByCompany(2)).thenThrow(new IllegalArgumentException("Not found"));
        ResponseEntity<PurchasePolicyController.PolicyResponse> resCompanyEx = controller.getPolicyByCompany(2);
        assertEquals(HttpStatus.NOT_FOUND, resCompanyEx.getStatusCode());
        
        // PolicyResponse setter test
        PurchasePolicyController.PolicyResponse responseObj = new PurchasePolicyController.PolicyResponse("desc");
        responseObj.setDescription("new desc");
        assertEquals("new desc", responseObj.getDescription());
    }
}
