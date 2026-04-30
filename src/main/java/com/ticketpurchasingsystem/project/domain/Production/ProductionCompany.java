package com.ticketpurchasingsystem.project.domain.Production;

import java.util.ArrayList;
import java.util.List;

import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.DiscountPolicy.DiscountPolicy;
import com.ticketpurchasingsystem.project.domain.Production.ProductionPolicy.PurchasePolicy.PurchasePolicy;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;

public class ProductionCompany {
    private Integer companyId;
    private String companyName;
    private String companyEmail;
    private String companyDescription;
    private String founderId;
    private List<String> ownerIds;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;

    public ProductionCompany(ProductionCompanyDTO dto) {
        this.companyName = dto.getCompanyName();
        this.companyEmail = dto.getCompanyEmail();
        this.companyDescription = dto.getCompanyDescription();
        this.ownerIds = new ArrayList<>();
        this.purchasePolicy = new PurchasePolicy();
        this.discountPolicy = new DiscountPolicy();
    }

    public void initFounder(String userId) {
        this.founderId = userId;
        if (!this.ownerIds.contains(userId)) {
            this.ownerIds.add(userId);
        }
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
