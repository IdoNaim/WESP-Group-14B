package com.ticketpurchasingsystem.project.domain.Production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy.DiscountPolicy;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.IPurchaseRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchasePolicy;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.AndRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MaxAgeRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MaxTicketsRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinAgeRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.MinTicketsRule;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.rules.OrRule;
import com.ticketpurchasingsystem.project.domain.Utils.ManagerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.OwnerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.tickets.ITicketPurchaseRule;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
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
    private List<UserProductionCompany> members = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    @OrderBy("position ASC")
    private List<ProductionPurchaseRule> purchaseRules = new ArrayList<>();

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
        this.members = new ArrayList<>();
        this.purchasePolicy = new PurchasePolicy();
        this.discountPolicy = new DiscountPolicy();
    }

    public ProductionCompany(ProductionCompanyDTO dto) {
        this.companyName = dto.getCompanyName();
        this.companyEmail = dto.getCompanyEmail();
        this.companyDescription = dto.getCompanyDescription();
        this.members = new ArrayList<>();
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
        this.members = new ArrayList<>();
        for (UserProductionCompany m : other.members) {
            this.members.add(new UserProductionCompany(
                    m.getId(), m.getUserId(), m.getRole(), m.getAppointerId(), m.getPermission(),
                    m.getStatus(), this));
        }
        this.purchaseRules = new ArrayList<>(other.purchaseRules);
        this.purchasePolicy = other.purchasePolicy;
        this.discountPolicy = other.discountPolicy;
        this.version = other.version;
        this.ticketPurchasePolicy = other.ticketPurchasePolicy;
    }

    // ── Founder / Owner ─────────────────────────────────────────────────────

    public void initFounder(String userId) {
        this.founderId = userId;
        if (members.stream().noneMatch(m -> m.getUserId().equals(userId)
                && m.getRole() == UserProductionCompany.MemberRole.OWNER
                && m.getPermission() == null)) {
            members.add(new UserProductionCompany(
                    userId, UserProductionCompany.MemberRole.OWNER, null, null, this));
        }
    }

    /** Add an active owner directly (used on appointment acceptance and by fixtures). */
    public boolean appointOwner(String appointerId, String appointeeId) {
        if (isOwner(appointeeId)) return false;
        members.add(new UserProductionCompany(
                appointeeId, UserProductionCompany.MemberRole.OWNER, appointerId, null, this));
        return true;
    }

    /** Create a pending owner appointment request the appointee must accept. */
    public boolean requestOwner(String appointerId, String appointeeId) {
        if (isOwner(appointeeId) || hasPendingAppointment(appointeeId)) return false;
        members.add(new UserProductionCompany(
                appointeeId, UserProductionCompany.MemberRole.OWNER, appointerId, null,
                UserProductionCompany.MemberStatus.PENDING, this));
        return true;
    }

    public boolean isOwner(String userId) {
        return members.stream().anyMatch(m -> m.getUserId().equals(userId)
                && m.getRole() == UserProductionCompany.MemberRole.OWNER
                && m.getPermission() == null
                && m.isActive());
    }

    public boolean isFounder(String userId) {
        return userId != null && userId.equals(founderId);
    }

    public Optional<OwnerDTO> getOwnerDTO(String userId) {
        return members.stream()
                .filter(m -> m.getUserId().equals(userId)
                        && m.getRole() == UserProductionCompany.MemberRole.OWNER
                        && m.getPermission() == null
                        && m.isActive())
                .map(m -> new OwnerDTO(m.getUserId(), m.getAppointerId()))
                .findFirst();
    }

    public Map<String, OwnerDTO> getOwnershipTree() {
        Map<String, OwnerDTO> tree = new LinkedHashMap<>();
        for (UserProductionCompany m : members) {
            if (m.getRole() == UserProductionCompany.MemberRole.OWNER && m.getPermission() == null
                    && m.isActive()) {
                tree.put(m.getUserId(), new OwnerDTO(m.getUserId(), m.getAppointerId()));
            }
        }
        return Collections.unmodifiableMap(tree);
    }

    public List<String> getOwnerIds() {
        return members.stream()
                .filter(m -> m.getRole() == UserProductionCompany.MemberRole.OWNER
                        && m.getPermission() == null
                        && m.isActive())
                .map(UserProductionCompany::getUserId)
                .collect(Collectors.toList());
    }

    public void addOwnerId(String ownerId) {
        if (members.stream().noneMatch(m -> m.getUserId().equals(ownerId)
                && m.getRole() == UserProductionCompany.MemberRole.OWNER
                && m.getPermission() == null)) {
            members.add(new UserProductionCompany(
                    ownerId, UserProductionCompany.MemberRole.OWNER, null, null, this));
        }
    }

    public boolean removeOwner(String requesterId, String ownerId) {
        if (isFounder(ownerId)) return false;
        if (!isOwner(requesterId) && !isFounder(requesterId)) return false;
        if (!isOwner(ownerId)) return false;
        // Remove base ownership row only; permission rows are left (matches original behaviour)
        members.removeIf(m -> m.getUserId().equals(ownerId)
                && m.getRole() == UserProductionCompany.MemberRole.OWNER
                && m.getPermission() == null);
        return true;
    }

    public boolean isAppointedBy(String memberId, String appointerId) {
        return members.stream().anyMatch(m -> m.getUserId().equals(memberId)
                && m.getRole() == UserProductionCompany.MemberRole.OWNER
                && appointerId.equals(m.getAppointerId())
                && m.getPermission() == null
                && m.isActive());
    }

    // ── Manager ──────────────────────────────────────────────────────────────

    /** Add an active manager directly (used on appointment acceptance and by fixtures). */
    public boolean appointManager(String appointerId, String managerId, Set<ManagerPermission> permissions) {
        if (isManager(managerId)) return false;
        // Base membership row
        members.add(new UserProductionCompany(
                managerId, UserProductionCompany.MemberRole.MANAGER, appointerId, null, this));
        // One permission row per granted permission
        for (ManagerPermission perm : permissions) {
            members.add(new UserProductionCompany(
                    managerId, UserProductionCompany.MemberRole.MANAGER, appointerId, perm, this));
        }
        return true;
    }
    // public boolean appointManager(String appointerId, String managerId, Set<ManagerPermission> permissions) {
    //     if (isManager(managerId)) return false;

    //     if (permissions == null || permissions.isEmpty()) {
    //         members.add(new UserProductionCompany(
    //                 managerId, UserProductionCompany.MemberRole.MANAGER, appointerId, null, this));
    //     } 
    //     else {
    //         for (ManagerPermission perm : permissions) {
    //             members.add(new UserProductionCompany(
    //                     managerId, UserProductionCompany.MemberRole.MANAGER, appointerId, perm, this));
    //         }
    //     }
    //     return true;
    // }

    /** Create a pending manager appointment request the appointee must accept. */
    public boolean requestManager(String appointerId, String managerId, Set<ManagerPermission> permissions) {
        if (isManager(managerId) || hasPendingAppointment(managerId)) return false;
        // Base membership row (PENDING until the appointee accepts)
        members.add(new UserProductionCompany(
                managerId, UserProductionCompany.MemberRole.MANAGER, appointerId, null,
                UserProductionCompany.MemberStatus.PENDING, this));
        // One PENDING permission row per requested permission
        for (ManagerPermission perm : permissions) {
            members.add(new UserProductionCompany(
                    managerId, UserProductionCompany.MemberRole.MANAGER, appointerId, perm,
                    UserProductionCompany.MemberStatus.PENDING, this));
        }
        return true;
    }

    public boolean isManager(String userId) {
      if (userId == null || members == null) return false;
        return members.stream()
                .anyMatch(m -> userId.equals(m.getUserId()) && 
                            m.getRole() == UserProductionCompany.MemberRole.MANAGER);
    }

    public boolean isOwnerOrManager(String userId) {
        return isOwner(userId) || isManager(userId);
    }

    public Optional<ManagerDTO> getManagerDTO(String userId) {
        if (!isManager(userId)) return Optional.empty();
                String appointerId = members.stream()
                .filter(m -> m.getUserId().equals(userId) && m.getRole() == UserProductionCompany.MemberRole.MANAGER)
                .map(UserProductionCompany::getAppointerId)
                .findFirst()
                .orElse(null);
                
        return Optional.of(new ManagerDTO(userId, appointerId, getManagerPermissions(userId)));
    }

    public Map<String, ManagerDTO> getManagerTree() {
        Map<String, ManagerDTO> tree = new LinkedHashMap<>();
        
        Set<String> managerIds = members.stream()
                .filter(m -> m.getRole() == UserProductionCompany.MemberRole.MANAGER)
                .map(UserProductionCompany::getUserId)
                .collect(Collectors.toSet());
                
        for (String managerId : managerIds) {
            String appointerId = members.stream()
                    .filter(m -> m.getUserId().equals(managerId) && m.getRole() == UserProductionCompany.MemberRole.MANAGER)
                    .map(UserProductionCompany::getAppointerId)
                    .findFirst()
                    .orElse(null);
            tree.put(managerId, new ManagerDTO(managerId, appointerId, getManagerPermissions(managerId)));
        }
        return Collections.unmodifiableMap(tree);
    }

    public boolean isManagerAppointedByOwner(String managerId, String ownerId) {
        return members.stream().anyMatch(m -> m.getUserId().equals(managerId)
                && m.getRole() == UserProductionCompany.MemberRole.MANAGER
                && ownerId.equals(m.getAppointerId()));
    }

    public boolean removeManager(String appointerId, String managerId) {
        if (!isManager(managerId) || !isManagerAppointedByOwner(managerId, appointerId)) return false;
        // Remove all rows for this manager (base + permissions)
        members.removeIf(m -> m.getUserId().equals(managerId)
                && m.getRole() == UserProductionCompany.MemberRole.MANAGER);
        return true;
    }

    // ── Appointment consent ────────────────────────────────────────────────────

    /** True if the user has a not-yet-answered appointment request for this company. */
    public boolean hasPendingAppointment(String userId) {
        return members.stream().anyMatch(m -> m.getUserId().equals(userId)
                && m.getPermission() == null
                && m.isPending());
    }

    /** The role the user was invited to, or empty if there is no pending request. */
    public Optional<UserProductionCompany.MemberRole> getPendingRole(String userId) {
        return members.stream()
                .filter(m -> m.getUserId().equals(userId) && m.getPermission() == null && m.isPending())
                .map(UserProductionCompany::getRole)
                .findFirst();
    }

    /** The appointer of the user's pending request, or empty if there is none. */
    public Optional<String> getPendingAppointerId(String userId) {
        return members.stream()
                .filter(m -> m.getUserId().equals(userId) && m.getPermission() == null && m.isPending())
                .map(UserProductionCompany::getAppointerId)
                .findFirst();
    }

    /** The permissions attached to the user's pending manager request (empty for owners). */
    public Set<ManagerPermission> getPendingPermissions(String userId) {
        return members.stream()
                .filter(m -> m.getUserId().equals(userId) && m.getPermission() != null && m.isPending())
                .map(UserProductionCompany::getPermission)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Accept a pending appointment: drop the pending rows and add the user as an
     * active member, reusing the same role, appointer, and permissions that were
     * requested.
     */
    public boolean acceptAppointment(String userId) {
        Optional<UserProductionCompany.MemberRole> role = getPendingRole(userId);
        if (role.isEmpty()) return false;
        String appointerId = getPendingAppointerId(userId).orElse(null);
        Set<ManagerPermission> permissions = getPendingPermissions(userId);

        denyAppointment(userId); // remove the pending rows first
        if (role.get() == UserProductionCompany.MemberRole.OWNER) {
            return appointOwner(appointerId, userId);
        }
        return appointManager(appointerId, userId, permissions);
    }

    /** Deny a pending appointment: remove every pending row for the user. */
    public boolean denyAppointment(String userId) {
        if (!hasPendingAppointment(userId)) return false;
        members.removeIf(m -> m.getUserId().equals(userId) && m.isPending());
        return true;
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    // ── Permissions ──────────────────────────────────────────────────────────

    // ── Permissions ──────────────────────────────────────────────────────────

    public boolean setManagerPermissions(String userId, Set<ManagerPermission> permissions) {
        Optional<UserProductionCompany> referenceRowOpt = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst();
                
        if (referenceRowOpt.isEmpty()) {
            return false; 
        }
        
        UserProductionCompany referenceRow = referenceRowOpt.get();

        members.removeIf(m -> m.getUserId().equals(userId) && m.getPermission() != null);

        boolean hasBaseRow = members.stream()
                .anyMatch(m -> m.getUserId().equals(userId) && m.getPermission() == null);
                
        if (!hasBaseRow) {
            members.add(new UserProductionCompany(
                    userId, referenceRow.getRole(), referenceRow.getAppointerId(), null, this));
        }

        if (permissions != null && !permissions.isEmpty()) {
            for (ManagerPermission perm : permissions) {
                members.add(new UserProductionCompany(
                        userId, referenceRow.getRole(), referenceRow.getAppointerId(), perm, this));
            }
        }
        return true;
    }

    public Set<ManagerPermission> getManagerPermissions(String userId) {
        Set<ManagerPermission> perms = members.stream()
                .filter(m -> m.getUserId().equals(userId) && m.getPermission() != null && m.isActive())
                .map(UserProductionCompany::getPermission)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return perms.isEmpty()
                ? ManagerPermission.none()
                : Collections.unmodifiableSet(perms);
    }

    // ── Purchase Policy ──────────────────────────────────────────────────────

    @PostLoad
    private void onPostLoad() {
        this.purchasePolicy = new PurchasePolicy();
        for (ProductionPurchaseRule ruleEntity : purchaseRules) {
            purchasePolicy.addRule(buildRuleFromEntity(ruleEntity));
        }
    }

    public void addPurchaseRule(IPurchaseRule rule) {
        if (purchasePolicy == null) purchasePolicy = new PurchasePolicy();
        purchasePolicy.addRule(rule);
        ProductionPurchaseRule entity = buildRuleEntity(rule, purchaseRules.size());
        purchaseRules.add(entity);
    }

    private ProductionPurchaseRule buildRuleEntity(IPurchaseRule rule, int position) {
        ProductionPurchaseRule entity = new ProductionPurchaseRule(
                toEntityRuleType(rule), toIntValue(rule), position);
        List<IPurchaseRule> children = toChildRules(rule);
        for (int i = 0; i < children.size(); i++) {
            entity.addChild(buildRuleEntity(children.get(i), i));
        }
        return entity;
    }

    private IPurchaseRule buildRuleFromEntity(ProductionPurchaseRule entity) {
        return switch (entity.getRuleType()) {
            case MIN_AGE -> new MinAgeRule(entity.getIntValue());
            case MAX_AGE -> new MaxAgeRule(entity.getIntValue());
            case MIN_TICKETS -> new MinTicketsRule(entity.getIntValue());
            case MAX_TICKETS -> new MaxTicketsRule(entity.getIntValue());
            case AND -> {
                IPurchaseRule[] children = entity.getChildren().stream()
                        .sorted(Comparator.comparingInt(ProductionPurchaseRule::getPosition))
                        .map(this::buildRuleFromEntity)
                        .toArray(IPurchaseRule[]::new);
                yield new AndRule(children);
            }
            case OR -> {
                IPurchaseRule[] children = entity.getChildren().stream()
                        .sorted(Comparator.comparingInt(ProductionPurchaseRule::getPosition))
                        .map(this::buildRuleFromEntity)
                        .toArray(IPurchaseRule[]::new);
                yield new OrRule(children);
            }
        };
    }

    private ProductionPurchaseRule.RuleType toEntityRuleType(IPurchaseRule rule) {
        if (rule instanceof MinAgeRule) return ProductionPurchaseRule.RuleType.MIN_AGE;
        if (rule instanceof MaxAgeRule) return ProductionPurchaseRule.RuleType.MAX_AGE;
        if (rule instanceof MinTicketsRule) return ProductionPurchaseRule.RuleType.MIN_TICKETS;
        if (rule instanceof MaxTicketsRule) return ProductionPurchaseRule.RuleType.MAX_TICKETS;
        if (rule instanceof AndRule) return ProductionPurchaseRule.RuleType.AND;
        if (rule instanceof OrRule) return ProductionPurchaseRule.RuleType.OR;
        throw new IllegalArgumentException("Unknown rule type: " + rule.getClass().getSimpleName());
    }

    private Integer toIntValue(IPurchaseRule rule) {
        if (rule instanceof MinAgeRule r) return r.getMinimumAge();
        if (rule instanceof MaxAgeRule r) return r.getMaximumAge();
        if (rule instanceof MinTicketsRule r) return r.getMinimum();
        if (rule instanceof MaxTicketsRule r) return r.getLimit();
        return null;
    }

    private List<IPurchaseRule> toChildRules(IPurchaseRule rule) {
        if (rule instanceof AndRule r) return r.getRules();
        if (rule instanceof OrRule r) return r.getRules();
        return Collections.emptyList();
    }

    public void setPurchasePolicy(PurchasePolicyDTO dto) {
        PurchasePolicy policy = new PurchasePolicy();

        IPurchaseRule ageBlock = null;
        if (dto.minAge() != null && dto.maxAge() != null) {
            IPurchaseRule minA = new MinAgeRule(dto.minAge());
            IPurchaseRule maxA = new MaxAgeRule(dto.maxAge());
            ageBlock = dto.isAgeOr() ? new OrRule(minA, maxA) : new AndRule(minA, maxA);
        } else if (dto.minAge() != null) {
            ageBlock = new MinAgeRule(dto.minAge());
        } else if (dto.maxAge() != null) {
            ageBlock = new MaxAgeRule(dto.maxAge());
        }

        IPurchaseRule quantityBlock = null;
        if (dto.minTickets() != null && dto.maxTickets() != null) {
            IPurchaseRule minT = new MinTicketsRule(dto.minTickets());
            IPurchaseRule maxT = new MaxTicketsRule(dto.maxTickets());
            quantityBlock = dto.isQuantityOr() ? new OrRule(minT, maxT) : new AndRule(minT, maxT);
        } else if (dto.minTickets() != null) {
            quantityBlock = new MinTicketsRule(dto.minTickets());
        } else if (dto.maxTickets() != null) {
            quantityBlock = new MaxTicketsRule(dto.maxTickets());
        }

        if (ageBlock != null && quantityBlock != null) {
            IPurchaseRule root = dto.isAgeAndQuantityOr()
                    ? new OrRule(ageBlock, quantityBlock)
                    : new AndRule(ageBlock, quantityBlock);
            policy.addRule(root);
        } else if (ageBlock != null) {
            policy.addRule(ageBlock);
        } else if (quantityBlock != null) {
            policy.addRule(quantityBlock);
        }

        this.purchasePolicy = policy;
        this.purchaseRules.clear();
        List<IPurchaseRule> rules = policy.getRules();
        for (int i = 0; i < rules.size(); i++) {
            purchaseRules.add(buildRuleEntity(rules.get(i), i));
        }
    }

    public PurchasePolicyDTO getPurchasePolicyDTO() {
        if (purchasePolicy == null || purchasePolicy.getRules().isEmpty()) {
            return new PurchasePolicyDTO(null, null, false, null, null, false, false);
        }
        ProdRuleExtractor extractor = new ProdRuleExtractor();
        extractor.extract(purchasePolicy.getRules().get(0));
        return extractor.toDTO();
    }

    private static class ProdRuleExtractor {
        Integer minAge, maxAge, minTickets, maxTickets;
        Boolean isAgeOr, isQuantityOr, isAgeAndQuantityOr;

        void extract(IPurchaseRule rule) {
            if (rule instanceof MinAgeRule r)      { minAge      = r.getMinimumAge(); }
            else if (rule instanceof MaxAgeRule r) { maxAge      = r.getMaximumAge(); }
            else if (rule instanceof MinTicketsRule r) { minTickets = r.getMinimum(); }
            else if (rule instanceof MaxTicketsRule r) { maxTickets = r.getLimit();   }
            else if (rule instanceof OrRule r)  { extractBinary(r.getRules(), true);  }
            else if (rule instanceof AndRule r) { extractBinary(r.getRules(), false); }
        }

        void extractBinary(List<IPurchaseRule> children, boolean or) {
            if (children.size() < 2) return;
            IPurchaseRule left = children.get(0), right = children.get(1);
            if (isAge(left) && isAge(right))  { isAgeOr = or;              extract(left); extract(right); }
            else if (isQty(left) && isQty(right)) { isQuantityOr = or;     extract(left); extract(right); }
            else                              { isAgeAndQuantityOr = or;    extract(left); extract(right); }
        }

        boolean isAge(IPurchaseRule r) {
            return r instanceof MinAgeRule || r instanceof MaxAgeRule
                || (r instanceof AndRule a && !a.getRules().isEmpty() && isAge(a.getRules().get(0)))
                || (r instanceof OrRule  o && !o.getRules().isEmpty() && isAge(o.getRules().get(0)));
        }

        boolean isQty(IPurchaseRule r) {
            return r instanceof MinTicketsRule || r instanceof MaxTicketsRule
                || (r instanceof AndRule a && !a.getRules().isEmpty() && isQty(a.getRules().get(0)))
                || (r instanceof OrRule  o && !o.getRules().isEmpty() && isQty(o.getRules().get(0)));
        }

        PurchasePolicyDTO toDTO() {
            return new PurchasePolicyDTO(
                minTickets, maxTickets, Boolean.TRUE.equals(isQuantityOr),
                minAge,     maxAge,     Boolean.TRUE.equals(isAgeOr),
                Boolean.TRUE.equals(isAgeAndQuantityOr)
            );
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public String getCompanyEmail() { return companyEmail; }
    public String getCompanyDescription() { return companyDescription; }

    public String getFounderId() { return founderId; }
    public void setFounderId(String founderId) { this.founderId = founderId; }

    public PurchasePolicy getPurchasePolicy() { return purchasePolicy; }
    public DiscountPolicy getDiscountPolicy() { return discountPolicy; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public ITicketPurchaseRule getTicketPurchasePolicy() { return ticketPurchasePolicy; }
    public void setTicketPurchasePolicy(ITicketPurchaseRule ticketPurchasePolicy) {
        this.ticketPurchasePolicy = ticketPurchasePolicy;
    }
}
