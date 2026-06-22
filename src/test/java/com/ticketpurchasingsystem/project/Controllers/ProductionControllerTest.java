package com.ticketpurchasingsystem.project.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketpurchasingsystem.project.Controllers.apidto.AppointManagerRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.AssignOwnerRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.ModifyPermissionsRequestDTO;
import com.ticketpurchasingsystem.project.application.IProductionService;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductionController.class)
@Import(SecurityConfig.class)
class ProductionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IProductionService productionService;

    private static final String VALID_AUTH = "Bearer valid-token";
    private static final String VALID_TOKEN = "valid-token";

    // create production company
    // POST /api/production/companies
    @Test
    void GivenValidRequest_WhenCreateCompany_ThenReturn201WithMessageAndCompanyId() throws Exception {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Eden Events", "Great events", "eden@events.com");
        when(productionService.createProductionCompany(eq(VALID_TOKEN), any())).thenReturn(2);

        mockMvc.perform(post("/api/production/companies")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Production company created successfully."))
                .andExpect(jsonPath("$.companyId").value("2"));
    }

    @Test
    void GivenInvalidToken_WhenCreateCompany_ThenReturn400WithError() throws Exception {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Eden Events", "desc", "eden@events.com");
        when(productionService.createProductionCompany(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/production/companies")
                .header("Authorization", "Bearer bad-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
    // assign owner
    // POST /api/production/companies/{companyId}/owners

    @Test
    void GivenValidRequest_WhenAssignOwner_ThenReturn200WithMessage() throws Exception {
        AssignOwnerRequestDTO dto = new AssignOwnerRequestDTO();
        dto.setAppointeeUserId("new-owner");
        when(productionService.assignOwner(VALID_TOKEN, 1, "new-owner")).thenReturn(true);

        mockMvc.perform(post("/api/production/companies/1/owners")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Owner appointment request sent."));
    }

    @Test
    void GivenUnauthorized_WhenAssignOwner_ThenReturn400WithError() throws Exception {
        AssignOwnerRequestDTO dto = new AssignOwnerRequestDTO();
        dto.setAppointeeUserId("new-owner");
        when(productionService.assignOwner(any(), any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/production/companies/1/owners")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // appoint manager
    // POST /api/production/companies/{companyId}/managers
    @Test
    void GivenValidRequest_WhenAppointManager_ThenReturn201WithMessage() throws Exception {
        AppointManagerRequestDTO dto = new AppointManagerRequestDTO();
        dto.setManagerId("manager-user");
        dto.setPermissions(EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT));
        when(productionService.appointManager(eq(VALID_TOKEN), eq(1), eq("manager-user"), any())).thenReturn(true);

        mockMvc.perform(post("/api/production/companies/1/managers")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Manager appointment request sent."));
    }

    @Test
    void GivenUnauthorized_WhenAppointManager_ThenReturn400WithError() throws Exception {
        AppointManagerRequestDTO dto = new AppointManagerRequestDTO();
        dto.setManagerId("manager-user");
        dto.setPermissions(EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT));
        when(productionService.appointManager(any(), any(), any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/production/companies/1/managers")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
    // modify manager permissions
    // PUT /api/production/companies/{companyId}/managers/{managerId}/permissions

    @Test
    void GivenValidRequest_WhenModifyPermissions_ThenReturn200WithMessage() throws Exception {
        ModifyPermissionsRequestDTO dto = new ModifyPermissionsRequestDTO();
        dto.setPermissions(
                EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT, ManagerPermission.SALES_REPORT_GENERATION));
        when(productionService.modifyManagerPermissions(eq(VALID_TOKEN), eq(1), eq("manager-1"), any()))
                .thenReturn(true);

        mockMvc.perform(put("/api/production/companies/1/managers/manager-1/permissions")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Manager permissions updated successfully."));
    }

    @Test
    void GivenUnauthorized_WhenModifyPermissions_ThenReturn400WithError() throws Exception {
        ModifyPermissionsRequestDTO dto = new ModifyPermissionsRequestDTO();
        dto.setPermissions(EnumSet.of(ManagerPermission.INVENTORY_MANAGEMENT));
        when(productionService.modifyManagerPermissions(any(), any(), any(), any())).thenReturn(false);

        mockMvc.perform(put("/api/production/companies/1/managers/manager-1/permissions")
                .header("Authorization", VALID_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // remove manager
    // DELETE /api/production/companies/{companyId}/managers/{managerId}
    @Test
    void GivenValidRequest_WhenRemoveManager_ThenReturn200WithMessage() throws Exception {
        when(productionService.removeManager(VALID_TOKEN, 1, "manager-1")).thenReturn(true);

        mockMvc.perform(delete("/api/production/companies/1/managers/manager-1")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Manager removed successfully."));
    }

    @Test
    void GivenUnauthorized_WhenRemoveManager_ThenReturn400WithError() throws Exception {
        when(productionService.removeManager(any(), any(), any())).thenReturn(false);

        mockMvc.perform(delete("/api/production/companies/1/managers/manager-1")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // get roles tree
    // GET /api/production/companies/{companyId}/roles
    @Test
    void GivenValidRequest_WhenGetRolesTree_ThenReturn200WithBody() throws Exception {
        RolesTreeDTO rolesTree = new RolesTreeDTO(1, "Test Company", "founder-user", Map.of(), Map.of(), Map.of());
        when(productionService.getRolesTree(VALID_TOKEN, 1)).thenReturn(rolesTree);

        mockMvc.perform(get("/api/production/companies/1/roles")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(1))
                .andExpect(jsonPath("$.founderId").value("founder-user"));
    }

    @Test
    void GivenUnauthorized_WhenGetRolesTree_ThenReturn403() throws Exception {
        when(productionService.getRolesTree(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/production/companies/1/roles")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isForbidden());
    }

    // get purchase history
    // GET /api/production/companies/{companyId}/history

    @Test
    void GivenValidRequest_WhenGetPurchaseHistory_ThenReturn200WithEmptyList() throws Exception {
        when(productionService.getCompanyPurchaseHistory(VALID_TOKEN, 1)).thenReturn(List.of());

        mockMvc.perform(get("/api/production/companies/1/history")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void GivenUnauthorized_WhenGetPurchaseHistory_ThenReturn403() throws Exception {
        when(productionService.getCompanyPurchaseHistory(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/production/companies/1/history")
                .header("Authorization", VALID_AUTH))
                .andExpect(status().isForbidden());
    }
}
