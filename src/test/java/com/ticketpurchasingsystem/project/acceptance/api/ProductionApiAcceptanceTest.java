package com.ticketpurchasingsystem.project.acceptance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.Controllers.ProductionController;
import com.ticketpurchasingsystem.project.Controllers.apidto.AppointManagerRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.AssignOwnerRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.ModifyPermissionsRequestDTO;
import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEvents.IsUserRegisteredEvent;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance tests for the ProductionController HTTP layer.
 * Uses real services wired without Spring context — same pattern as existing
 * acceptance tests
 * but verified through HTTP requests instead of direct service calls.
 */
class ProductionApiAcceptanceTest {

    private static final String TEST_SECRET = "my-test-secret-key-for-jwt-testing-only!";
    private static final String FOUNDER = "founder-eden";
    private static final String MANAGER_ID = "manager-itay";

    private final Set<String> registeredUsers = new HashSet<>();

    private AuthenticationService authService;
    private ProdRepo prodRepo;
    private ProductionService productionService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private int companyId;

    @BeforeEach
    void setUp() {
        registeredUsers.clear();

        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuthService = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuthService, "secret", TEST_SECRET);
        domainAuthService.init();
        authService = new AuthenticationService(domainAuthService, sessionRepo);

        prodRepo = new ProdRepo();
        ProductionEventPublisher publisher = new ProductionEventPublisher(event -> {
            if (event instanceof IsUserRegisteredEvent e) {
                e.setRegistered(registeredUsers.contains(e.getUserId()));
            }
        });
        productionService = new ProductionService(authService, new ProductionHandler(), prodRepo, publisher);

        mockMvc = MockMvcBuilders.standaloneSetup(new ProductionController(productionService)).build();
        objectMapper = new ObjectMapper();

        // Pre-create a company for owner/manager tests
        String founderToken = authService.login(FOUNDER);
        productionService.createProductionCompany(founderToken,
                new ProductionCompanyDTO("Events Co", "desc", "events@co.com"));
        companyId = prodRepo.findByName("Events Co").get().getCompanyId();
    }

    // create production company
    // POST /api/production/companies

    @Test
    void GivenLoggedInUser_WhenCreateCompany_ThenReturn201WithMessageAndCompanyId() throws Exception {
        String token = authService.login("new-user");
        ProductionCompanyDTO dto = new ProductionCompanyDTO("My Shows", "Great shows", "my@shows.com");

        mockMvc.perform(post("/api/production/companies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Production company created successfully."))
                .andExpect(jsonPath("$.companyId").exists());
    }

    @Test
    void GivenInvalidToken_WhenCreateCompany_ThenReturn400WithError() throws Exception {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Ghost Co", "desc", "ghost@co.com");

        mockMvc.perform(post("/api/production/companies")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void GivenDuplicateCompanyName_WhenCreateCompany_ThenReturn400WithError() throws Exception {
        String token = authService.login(FOUNDER);
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Events Co", "desc", "events@co.com");

        mockMvc.perform(post("/api/production/companies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // assign owner
    // POST /api/production/companies/{companyId}/owners
    @Test
    void GivenFounderAssignsOwner_WhenAssignOwner_ThenReturn200WithMessage() throws Exception {
        registeredUsers.add("new-owner");
        String founderToken = authService.login(FOUNDER);
        AssignOwnerRequestDTO dto = new AssignOwnerRequestDTO();
        dto.setAppointeeUserId("new-owner");

        mockMvc.perform(post("/api/production/companies/" + companyId + "/owners")
                .header("Authorization", "Bearer " + founderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Owner assigned successfully."));
    }

    @Test
    void GivenNonOwner_WhenAssignOwner_ThenReturn400WithError() throws Exception {
        registeredUsers.add("new-owner");
        String nonOwnerToken = authService.login("random-user");
        AssignOwnerRequestDTO dto = new AssignOwnerRequestDTO();
        dto.setAppointeeUserId("new-owner");

        mockMvc.perform(post("/api/production/companies/" + companyId + "/owners")
                .header("Authorization", "Bearer " + nonOwnerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // appoint manager
    // POST /api/production/companies/{companyId}/managers
    @Test
    void GivenFounderAppointsManager_WhenAppointManager_ThenReturn201WithMessage() throws Exception {
        registeredUsers.add(MANAGER_ID);
        String founderToken = authService.login(FOUNDER);
        AppointManagerRequestDTO dto = new AppointManagerRequestDTO();
        dto.setManagerId(MANAGER_ID);
        dto.setPermissions(EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT));

        mockMvc.perform(post("/api/production/companies/" + companyId + "/managers")
                .header("Authorization", "Bearer " + founderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Manager appointed successfully."));
    }

    @Test
    void GivenUnregisteredUser_WhenAppointManager_ThenReturn400WithError() throws Exception {
        // MANAGER_ID NOT added to registeredUsers intentionally
        String founderToken = authService.login(FOUNDER);
        AppointManagerRequestDTO dto = new AppointManagerRequestDTO();
        dto.setManagerId(MANAGER_ID);
        dto.setPermissions(EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT));

        mockMvc.perform(post("/api/production/companies/" + companyId + "/managers")
                .header("Authorization", "Bearer " + founderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // modify permissions
    // PUT /api/production/companies/{companyId}/managers/{managerId}/permissions

    @Test
    void GivenOwnerModifiesCoOwnerPermissions_WhenModifyPermissions_ThenReturn200WithMessage() throws Exception {
        // modifyManagerPermissions operates on co-owners (assigned via assignOwner), not managers
        String coOwner = "co-owner-tomer";
        registeredUsers.add(coOwner);
        String founderToken = authService.login(FOUNDER);
        productionService.assignOwner(founderToken, companyId, coOwner);

        String newToken = authService.login(FOUNDER);
        ModifyPermissionsRequestDTO dto = new ModifyPermissionsRequestDTO();
        dto.setPermissions(EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT,
                ManagerPermission.PURCHASE_AND_ORDER_HISTORY_ACCESS));

        mockMvc.perform(put("/api/production/companies/" + companyId + "/managers/" + coOwner + "/permissions")
                .header("Authorization", "Bearer " + newToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Manager permissions updated successfully."));
    }

    @Test
    void GivenNonExistentManager_WhenModifyPermissions_ThenReturn400WithError() throws Exception {
        String founderToken = authService.login(FOUNDER);
        ModifyPermissionsRequestDTO dto = new ModifyPermissionsRequestDTO();
        dto.setPermissions(EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT));

        mockMvc.perform(put("/api/production/companies/" + companyId + "/managers/ghost-manager/permissions")
                .header("Authorization", "Bearer " + founderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // delete manager
    // DELETE /api/production/companies/{companyId}/managers/{managerId}
    @Test
    void GivenOwnerRemovesManager_WhenRemoveManager_ThenReturn200WithMessage() throws Exception {
        registeredUsers.add(MANAGER_ID);
        String founderToken = authService.login(FOUNDER);
        productionService.appointManager(founderToken, companyId, MANAGER_ID,
                EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT));

        String newToken = authService.login(FOUNDER);
        mockMvc.perform(delete("/api/production/companies/" + companyId + "/managers/" + MANAGER_ID)
                .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Manager removed successfully."));
    }

    @Test
    void GivenNonExistentManager_WhenRemoveManager_ThenReturn400WithError() throws Exception {
        String founderToken = authService.login(FOUNDER);

        mockMvc.perform(delete("/api/production/companies/" + companyId + "/managers/nobody")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // get roles tree
    // GET /api/production/companies/{companyId}/roles
    @Test
    void GivenOwner_WhenGetRolesTree_ThenReturn200WithCompanyData() throws Exception {
        String founderToken = authService.login(FOUNDER);

        mockMvc.perform(get("/api/production/companies/" + companyId + "/roles")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(companyId))
                .andExpect(jsonPath("$.founderId").value(FOUNDER));
    }

    @Test
    void GivenNonOwner_WhenGetRolesTree_ThenReturn403() throws Exception {
        String otherToken = authService.login("stranger");

        mockMvc.perform(get("/api/production/companies/" + companyId + "/roles")
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // get purchase history
    // GET /api/production/companies/{companyId}/history
    @Test
    void GivenOwner_WhenGetPurchaseHistory_ThenReturn200WithList() throws Exception {
        String founderToken = authService.login(FOUNDER);

        mockMvc.perform(get("/api/production/companies/" + companyId + "/history")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void GivenNonOwner_WhenGetPurchaseHistory_ThenReturn403() throws Exception {
        String otherToken = authService.login("stranger");

        mockMvc.perform(get("/api/production/companies/" + companyId + "/history")
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}
