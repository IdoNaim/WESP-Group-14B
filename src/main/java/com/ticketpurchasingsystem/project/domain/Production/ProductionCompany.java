package com.ticketpurchasingsystem.project.domain.Production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.ticketpurchasingsystem.project.domain.Production.ManagerPermission.none;
import java.util.Collections;
import java.util.HashSet;
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
    private final Map<String, Set<ManagerPermission>> managerPermissions;
    private long version;

    public ProductionCompany(ProductionCompanyDTO dto) {
        this.companyName = dto.getCompanyName();
        this.companyEmail = dto.getCompanyEmail();
        this.companyDescription = dto.getCompanyDescription();
        this.ownerIds = new ArrayList<>();
        this.ownershipTree = new LinkedHashMap<>();
        this.managerTree = new LinkedHashMap<>();
        this.purchasePolicy = new PurchasePolicy();
        this.discountPolicy = new DiscountPolicy();
        this.managerPermissions = new LinkedHashMap<>();
        this.version = 0;
    }

    public ProductionCompany(ProductionCompany other) {
        this.companyId = other.companyId;
        this.companyName = other.companyName;
        this.companyEmail = other.companyEmail;
        this.companyDescription = other.companyDescription;
        this.founderId = other.founderId;
        this.ownerIds = new ArrayList<>(other.ownerIds);
        this.ownershipTree = new LinkedHashMap<>(other.ownershipTree);
        this.managerTree = new LinkedHashMap<>(other.managerTree);
        this.purchasePolicy = other.purchasePolicy;
        this.discountPolicy = other.discountPolicy;
        this.managerPermissions = new LinkedHashMap<>();
        for (Map.Entry<String, Set<ManagerPermission>> entry : other.managerPermissions.entrySet()) {
            Set<ManagerPermission> permCopy = entry.getValue().isEmpty()
                    ? new HashSet<>()
                    : EnumSet.copyOf(entry.getValue());
            this.managerPermissions.put(entry.getKey(), permCopy);
        }
        this.version = other.version;
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
    public boolean removeManager(String appointerId, String managerId){
        if(!isManager(managerId) || !isManagerAppointedByOwner(managerId, appointerId)){
            return false;
        }
        managerTree.remove(managerId);
        if(managerPermissions.containsKey(managerId)) {
            managerPermissions.remove(managerId);
        }
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

    public void setManagerPermissions(String managerId, Set<ManagerPermission> permissions) {
        if (permissions.isEmpty()) {
            managerPermissions.put(managerId, none());
        } else {
            managerPermissions.put(managerId, EnumSet.copyOf(permissions));
        }
    }

    public Set<ManagerPermission> getManagerPermissions(String managerId) {
        return Collections.unmodifiableSet(
                managerPermissions.getOrDefault(managerId, none()));
    }

    public boolean isAppointedBy(String managerId, String ownerId) {
        return ownershipTree.containsKey(managerId)
                && ownerId.equals(ownershipTree.get(managerId).getAppointerId());
    }
    public boolean isManagerAppointedByOwner(String managerId, String ownerId){
        return managerTree.containsKey(managerId)
                && ownerId.equals(managerTree.get(managerId).getAppointerId());
    }
    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
