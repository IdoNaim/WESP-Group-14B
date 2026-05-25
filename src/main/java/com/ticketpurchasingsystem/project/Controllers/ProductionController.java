package com.ticketpurchasingsystem.project.Controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketpurchasingsystem.project.Controllers.apidto.AppointManagerRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.AssignOwnerRequestDTO;
import com.ticketpurchasingsystem.project.Controllers.apidto.ModifyPermissionsRequestDTO;
import com.ticketpurchasingsystem.project.application.IProductionService;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.RolesTreeDTO;

@RestController
@RequestMapping("/api/production")
public class ProductionController {

        private final IProductionService productionService;

        public ProductionController(IProductionService productionService) {
                this.productionService = productionService;
        }

        // POST /api/production/companies
        @PostMapping("/companies")
        public ResponseEntity<Map<String, String>> createProductionCompany(
                        @RequestHeader("Authorization") String authHeader,
                        @RequestBody ProductionCompanyDTO body) {

                String token = extractToken(authHeader);
                Integer companyId = productionService.createProductionCompany(token, body);
                if (companyId != null) {
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(Map.of(
                                                        "message", "Production company created successfully.",
                                                        "companyId", companyId.toString()));
                }
                return ResponseEntity.badRequest()
                                .body(Map.of("error",
                                                "Failed to create production company. Name may already be taken or token is invalid."));
        }

        // POST /api/production/companies/{companyId}/owners
        @PostMapping("/companies/{companyId}/owners")
        public ResponseEntity<Map<String, String>> assignOwner(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId,
                        @RequestBody AssignOwnerRequestDTO body) {

                String token = extractToken(authHeader);
                boolean success = productionService.assignOwner(token, companyId, body.getAppointeeUserId());
                if (success) {
                        return ResponseEntity.ok(Map.of("message", "Owner assigned successfully."));
                }
                return ResponseEntity.badRequest()
                                .body(Map.of("error",
                                                "Failed to assign owner. You may not have permission or the user does not exist."));
        }

        // POST /api/production/companies/{companyId}/managers
        @PostMapping("/companies/{companyId}/managers")
        public ResponseEntity<Map<String, String>> appointManager(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId,
                        @RequestBody AppointManagerRequestDTO body) {

                String token = extractToken(authHeader);
                boolean success = productionService.appointManager(
                                token, companyId, body.getManagerId(), body.getPermissions());
                if (success) {
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(Map.of("message", "Manager appointed successfully."));
                }
                return ResponseEntity.badRequest()
                                .body(Map.of("error",
                                                "Failed to appoint manager. You may not have permission, the user does not exist, or is already a manager."));
        }

        // PUT /api/production/companies/{companyId}/managers/{managerId}/permissions
        @PutMapping("/companies/{companyId}/managers/{managerId}/permissions")
        public ResponseEntity<Map<String, String>> modifyManagerPermissions(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId,
                        @PathVariable String managerId,
                        @RequestBody ModifyPermissionsRequestDTO body) {

                String token = extractToken(authHeader);
                boolean success = productionService.modifyManagerPermissions(
                                token, companyId, managerId, body.getPermissions());
                if (success) {
                        return ResponseEntity.ok(Map.of("message", "Manager permissions updated successfully."));
                }
                return ResponseEntity.badRequest()
                                .body(Map.of("error",
                                                "Failed to update permissions. You may not have permission or the manager does not exist."));
        }

        // DELETE /api/production/companies/{companyId}/managers/{managerId}
        @DeleteMapping("/companies/{companyId}/managers/{managerId}")
        public ResponseEntity<Map<String, String>> removeManager(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId,
                        @PathVariable String managerId) {

                String token = extractToken(authHeader);
                boolean success = productionService.removeManager(token, companyId, managerId);
                if (success) {
                        return ResponseEntity.ok(Map.of("message", "Manager removed successfully."));
                }
                return ResponseEntity.badRequest()
                                .body(Map.of("error",
                                                "Failed to remove manager. You may not have permission or the manager does not exist."));
        }

        // GET /api/production/companies/{companyId}/roles
        @GetMapping("/companies/{companyId}/roles")
        public ResponseEntity<RolesTreeDTO> getRolesTree(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId) {

                String token = extractToken(authHeader);
                RolesTreeDTO result = productionService.getRolesTree(token, companyId);
                return result != null
                                ? ResponseEntity.ok(result)
                                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // GET /api/production/companies/{companyId}/history
        @GetMapping("/companies/{companyId}/history")
        public ResponseEntity<List<HistoryOrderItem>> getCompanyPurchaseHistory(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId) {

                String token = extractToken(authHeader);
                List<HistoryOrderItem> history = productionService.getCompanyPurchaseHistory(token, companyId);
                return history != null
                                ? ResponseEntity.ok(history)
                                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        private String extractToken(String authHeader) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        return authHeader.substring(7);
                }
                return authHeader;
        }
}
