package com.ticketpurchasingsystem.project.domain.Production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy.DiscountPolicy;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchasePolicy;
import com.ticketpurchasingsystem.project.domain.Utils.ManagerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.OwnerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.tickets.ITicketPurchaseRule;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
@Table(name = "production_company")
public class ProductionCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "company_name", unique = true, nullable = false, length = 255)
    private String companyName;

    @Column(name = "company_email", length = 255)
    private String companyEmail;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "founder_id", length = 255)
    private String founderId;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<CompanyOwner> owners = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<CompanyManager> managers = new ArrayList<>();

    /**
     * Permissions for any company member (owner or manager).
     * One row per (user, permission) pair — mirrors the original managerPermissions map.
     */
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<CompanyMemberPermission> memberPermissions = new ArrayList<>();

    @Transient
    private PurchasePolicy purchasePolicy;

    @Transient
    private DiscountPolicy discountPolicy;

    @Version
    @Column(name = "version")
    private long version;

    @Transient
    private ITicketPurchaseRule ticketPurchasePolicy;

    protected ProductionCompany() {
        this.owners = new ArrayList<>();
        this.managers = new ArrayList<>();
        this.memberPermissions = new ArrayList<>();
        this.purchasePolicy = new PurchasePolicy();
        this.discountPolicy = new DiscountPolicy();
    }

    public ProductionCompany(ProductionCompanyDTO dto) {
        this.companyName = dto.getCompanyName();
        this.companyEmail = dto.getCompanyEmail();
        this.companyDescription = dto.getCompanyDescription();
        this.owners = new ArrayList<>();
        this.managers = new ArrayList<>();
        this.memberPermissions = new ArrayList<>();
        this.purchasePolicy = new PurchasePolicy();
        this.discountPolicy = new DiscountPolicy();
        this.version = 0;
    }

    public ProductionCompany(ProductionCompany other) {
        this.companyId = other.companyId;
        this.companyName = other.companyName;
        this.companyEmail = other.companyEmail;
        this.companyDescription = other.companyDescription;
        this.founderId = other.founderId;
        this.owners = new ArrayList<>();
        for (CompanyOwner o : other.owners) {
            this.owners.add(new CompanyOwner(o.getId(), o.getUserId(), o.getAppointerId(), this));
        }
        this.managers = new ArrayList<>();
        for (CompanyManager m : other.managers) {
            this.managers.add(new CompanyManager(m.getId(), m.getUserId(), m.getAppointerId(), this));
        }
        this.memberPermissions = new ArrayList<>();
        for (CompanyMemberPermission p : other.memberPermissions) {
            this.memberPermissions.add(
                    new CompanyMemberPermission(p.getId(), p.getUserId(), p.getPermission(), this));
        }
        this.purchasePolicy = other.purchasePolicy;
        this.discountPolicy = other.discountPolicy;
        this.version = other.version;
        this.ticketPurchasePolicy = other.ticketPurchasePolicy;
    }

    public void initFounder(String userId) {
        this.founderId = userId;
        if (owners.stream().noneMatch(o -> o.getUserId().equals(userId))) {
            owners.add(new CompanyOwner(userId, null, this));
        }
    }

    public boolean appointOwner(String appointerId, String appointeeId) {
        if (isOwner(appointeeId)) return false;
        owners.add(new CompanyOwner(appointeeId, appointerId, this));
        return true;
    }

    public boolean isOwner(String userId) {
        return owners.stream().anyMatch(o -> o.getUserId().equals(userId));
    }

    public boolean isFounder(String userId) {
        return userId != null && userId.equals(founderId);
    }

    public Optional<OwnerDTO> getOwnerDTO(String userId) {
        return owners.stream()
                .filter(o -> o.getUserId().equals(userId))
                .map(o -> new OwnerDTO(o.getUserId(), o.getAppointerId()))
                .findFirst();
    }

    public Map<String, OwnerDTO> getOwnershipTree() {
        Map<String, OwnerDTO> tree = new LinkedHashMap<>();
        for (CompanyOwner o : owners) {
            tree.put(o.getUserId(), new OwnerDTO(o.getUserId(), o.getAppointerId()));
        }
        return Collections.unmodifiableMap(tree);
    }

    public boolean appointManager(String appointerId, String managerId, Set<ManagerPermission> permissions) {
        if (isManager(managerId)) return false;
        managers.add(new CompanyManager(managerId, appointerId, this));
        for (ManagerPermission perm : permissions) {
            memberPermissions.add(new CompanyMemberPermission(managerId, perm, this));
        }
        return true;
    }

    public boolean removeOwner(String requesterId, String ownerId) {
        if (isFounder(ownerId)) return false;
        if (!isOwner(requesterId) && !isFounder(requesterId)) return false;
        if (!isOwner(ownerId)) return false;
        owners.removeIf(o -> o.getUserId().equals(ownerId));
        return true;
    }

    public boolean removeManager(String appointerId, String managerId) {
        if (!isManager(managerId) || !isManagerAppointedByOwner(managerId, appointerId)) {
            return false;
        }
        managers.removeIf(m -> m.getUserId().equals(managerId));
        memberPermissions.removeIf(p -> p.getUserId().equals(managerId));
        return true;
    }

    public boolean isManager(String userId) {
        return managers.stream().anyMatch(m -> m.getUserId().equals(userId));
    }

    public boolean isOwnerOrManager(String userId) {
        return isOwner(userId) || isManager(userId);
    }

    public Optional<ManagerDTO> getManagerDTO(String userId) {
        return managers.stream()
                .filter(m -> m.getUserId().equals(userId))
                .map(m -> new ManagerDTO(m.getUserId(), m.getAppointerId(), getManagerPermissions(userId)))
                .findFirst();
    }

    public Map<String, ManagerDTO> getManagerTree() {
        Map<String, ManagerDTO> tree = new LinkedHashMap<>();
        for (CompanyManager m : managers) {
            tree.put(m.getUserId(),
                    new ManagerDTO(m.getUserId(), m.getAppointerId(), getManagerPermissions(m.getUserId())));
        }
        return Collections.unmodifiableMap(tree);
    }

    public Integer getCompanyId() { return companyId; }

    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }

    public String getCompanyEmail() { return companyEmail; }

    public String getCompanyDescription() { return companyDescription; }

    public String getFounderId() { return founderId; }

    public void setFounderId(String founderId) { this.founderId = founderId; }

    public List<String> getOwnerIds() {
        return owners.stream().map(CompanyOwner::getUserId).collect(Collectors.toList());
    }

    public void addOwnerId(String ownerId) {
        if (owners.stream().noneMatch(o -> o.getUserId().equals(ownerId))) {
            owners.add(new CompanyOwner(ownerId, null, this));
        }
    }

    public PurchasePolicy getPurchasePolicy() { return purchasePolicy; }

    public DiscountPolicy getDiscountPolicy() { return discountPolicy; }

    public void setManagerPermissions(String userId, Set<ManagerPermission> permissions) {
        memberPermissions.removeIf(p -> p.getUserId().equals(userId));
        for (ManagerPermission perm : permissions) {
            memberPermissions.add(new CompanyMemberPermission(userId, perm, this));
        }
    }

    public Set<ManagerPermission> getManagerPermissions(String userId) {
        Set<ManagerPermission> perms = memberPermissions.stream()
                .filter(p -> p.getUserId().equals(userId))
                .map(CompanyMemberPermission::getPermission)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return perms.isEmpty()
                ? ManagerPermission.none()
                : Collections.unmodifiableSet(perms);
    }

    public boolean isAppointedBy(String memberId, String appointerId) {
        return owners.stream()
                .anyMatch(o -> o.getUserId().equals(memberId)
                        && appointerId.equals(o.getAppointerId()));
    }

    public boolean isManagerAppointedByOwner(String managerId, String ownerId) {
        return managers.stream()
                .anyMatch(m -> m.getUserId().equals(managerId)
                        && ownerId.equals(m.getAppointerId()));
    }

    public long getVersion() { return version; }

    public void setVersion(long version) { this.version = version; }

    public ITicketPurchaseRule getTicketPurchasePolicy() { return ticketPurchasePolicy; }

    public void setTicketPurchasePolicy(ITicketPurchaseRule ticketPurchasePolicy) {
        this.ticketPurchasePolicy = ticketPurchasePolicy;
    }
}
