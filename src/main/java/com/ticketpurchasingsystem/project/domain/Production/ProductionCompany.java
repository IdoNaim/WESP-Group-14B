package com.ticketpurchasingsystem.project.domain.Production;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.Set;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy.DiscountPolicy;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchasePolicy;
import com.ticketpurchasingsystem.project.domain.Utils.ManagerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.OwnerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

public class ProductionCompany {
    private Integer companyId;
    private String companyName;
    private String companyEmail;
    private String companyDescription;
    private String founderId;
    private List<String> ownerIds;
    private final Map<String, OwnerDTO> ownershipTree;
    private final Map<String, ManagerDTO> managerTree;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;

    public ProductionCompany(ProductionCompanyDTO dto) {
        this.companyName = dto.getCompanyName();
        this.companyEmail = dto.getCompanyEmail();
        this.companyDescription = dto.getCompanyDescription();
        this.ownerIds = new ArrayList<>();
        this.ownershipTree = new LinkedHashMap<>();
        this.managerTree = new LinkedHashMap<>();
        this.purchasePolicy = new PurchasePolicy();
        this.discountPolicy = new DiscountPolicy();
    }

    public void initFounder(String userId) {
        this.founderId = userId;
        if (!this.ownerIds.contains(userId)) {
            this.ownerIds.add(userId);
        }
        ownershipTree.putIfAbsent(userId, new OwnerDTO(userId, null)); // ← ADD THIS
    }

    public boolean appointOwner(String appointerId, String appointeeId) {
        if (ownerIds.contains(appointeeId)) {
            return false;
        }
        ownerIds.add(appointeeId);
        ownershipTree.put(appointeeId, new OwnerDTO(appointeeId, appointerId));
        return true;
    }

    public boolean isOwner(String userId) {
        return ownerIds.contains(userId);
    }

    public boolean isFounder(String userId) {
        return userId != null && userId.equals(founderId);
    }

    public Optional<OwnerDTO> getOwnerDTO(String userId) {
        return Optional.ofNullable(ownershipTree.get(userId));
    }

    public Map<String, OwnerDTO> getOwnershipTree() {
        return Collections.unmodifiableMap(ownershipTree);
    }

    public boolean appointManager(String appointerId, String managerId, Set<ManagerPermission> permissions) {
        if (managerTree.containsKey(managerId)) {
            return false;
        }
        managerTree.put(managerId, new ManagerDTO(managerId, appointerId, permissions));
        return true;
    }

    public boolean isManager(String userId) {
        return managerTree.containsKey(userId);
    }

    public boolean isOwnerOrManager(String userId) {
        return isOwner(userId) || isManager(userId);
    }

    public Optional<ManagerDTO> getManagerDTO(String userId) {
        return Optional.ofNullable(managerTree.get(userId));
    }

    public Map<String, ManagerDTO> getManagerTree() {
        return Collections.unmodifiableMap(managerTree);
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public String getFounderId() {
        return founderId;
    }

    public void setFounderId(String founderId) {
        this.founderId = founderId;
    }

    public List<String> getOwnerIds() {
        return ownerIds;
    }

    public void addOwnerId(String ownerId) {
        this.ownerIds.add(ownerId);
    }

    public PurchasePolicy getPurchasePolicy() {
        return purchasePolicy;
    }

    public DiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }
}
