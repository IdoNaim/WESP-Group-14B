package com.ticketpurchasingsystem.project.Controllers;

import java.util.List;

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
        public ResponseEntity<Integer> createProductionCompany(
                        @RequestHeader("Authorization") String authHeader,
                        @RequestBody ProductionCompanyDTO body) {

                String token = extractToken(authHeader);
                Integer companyId = productionService.createProductionCompany(token, body);
                return companyId != null
                                ? ResponseEntity.status(HttpStatus.CREATED).body(companyId)
                                : ResponseEntity.badRequest().build();
        }

        // POST /api/production/companies/{companyId}/owners
        @PostMapping("/companies/{companyId}/owners")
        public ResponseEntity<Void> assignOwner(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId,
                        @RequestBody AssignOwnerRequestDTO body) {

                String token = extractToken(authHeader);
                boolean success = productionService.assignOwner(token, companyId, body.getAppointeeUserId());
                return success
                                ? ResponseEntity.ok().build()
                                : ResponseEntity.badRequest().build();
        }

        // POST /api/production/companies/{companyId}/managers
        @PostMapping("/companies/{companyId}/managers")
        public ResponseEntity<Void> appointManager(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId,
                        @RequestBody AppointManagerRequestDTO body) {

                String token = extractToken(authHeader);
                boolean success = productionService.appointManager(
                                token, companyId, body.getManagerId(), body.getPermissions());
                return success
                                ? ResponseEntity.status(HttpStatus.CREATED).build()
                                : ResponseEntity.badRequest().build();
        }

        // PUT /api/production/companies/{companyId}/managers/{managerId}/permissions
        @PutMapping("/companies/{companyId}/managers/{managerId}/permissions")
        public ResponseEntity<Void> modifyManagerPermissions(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId,
                        @PathVariable String managerId,
                        @RequestBody ModifyPermissionsRequestDTO body) {

                String token = extractToken(authHeader);
                boolean success = productionService.modifyManagerPermissions(
                                token, companyId, managerId, body.getPermissions());
                return success
                                ? ResponseEntity.ok().build()
                                : ResponseEntity.badRequest().build();
        }

        // DELETE /api/production/companies/{companyId}/managers/{managerId}
        @DeleteMapping("/companies/{companyId}/managers/{managerId}")
        public ResponseEntity<Void> removeManager(
                        @RequestHeader("Authorization") String authHeader,
                        @PathVariable Integer companyId,
                        @PathVariable String managerId) {

                String token = extractToken(authHeader);
                boolean success = productionService.removeManager(token, companyId, managerId);
                return success
                                ? ResponseEntity.ok().build()
                                : ResponseEntity.badRequest().build();
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
