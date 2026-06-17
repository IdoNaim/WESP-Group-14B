package com.ticketpurchasingsystem.project.application;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderDTO;
import com.ticketpurchasingsystem.project.domain.ActiveOrders.ActiveOrderItem;
import com.ticketpurchasingsystem.project.domain.HistoryOrder.HistoryOrderItem;
import com.ticketpurchasingsystem.project.domain.Utils.HistoryOrderDTO;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.domain.User.UserDTO;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.User.UserInfo;
import com.ticketpurchasingsystem.project.domain.systemAdmin.AdminPublisher;
import com.ticketpurchasingsystem.project.infrastructure.logging.loggerDef;

@Service
public class SystemAdminService implements ISystemAdminService {

    private final IUserRepo userRepo;
    private final AdminPublisher adminPublisher;
    private final AuthenticationService authenticationService;
    private final loggerDef logger = loggerDef.getInstance();

    public SystemAdminService(IUserRepo userRepo,
                              AdminPublisher adminPublisher,
                              AuthenticationService authenticationService) {
        this.userRepo = userRepo;
        this.adminPublisher = adminPublisher;
        this.authenticationService = authenticationService;

        // admin-1 is seeded by UserService (which hashes the password via UserHandler).
        // Seeding here with a plain UserInfo constructor would store a plain-text
        // password and break BCrypt verification if this bean is constructed first.
    }

    @Override
    public List<ActiveOrderDTO> getAllActiveOrders(String token) {
        String adminId = validateAdminSession(token);
        List<ActiveOrderItem> orders = adminPublisher.publishGetAllActiveOrders(adminId);
        return orders.stream().map(ActiveOrderDTO::new).collect(Collectors.toList());
    }

    @Override
    public List<HistoryOrderDTO> getAllHistoryOrders(String token) {
        String adminId = validateAdminSession(token);
        List<HistoryOrderItem> history = adminPublisher.publishGetAllOrdersHistory(adminId);
        return history.stream().map(HistoryOrderItem::makeDTO).collect(Collectors.toList());
    }

    public List<UserInfo> getAllAdmins() {
        return userRepo.getAllUsers().stream()
                .filter(UserInfo::isAdmin)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getAllUsers(String token) {
        String adminId = validateAdminSession(token);
        return adminPublisher.publishGetAllUsers(adminId);
    }

    public String validateAdminSession(String token) {
        if (!authenticationService.validate(token)) {
            logger.error("Invalid session token for admin operation");
            throw new RuntimeException("Invalid session token");
        }
        String userId = authenticationService.getUser(token);
        if (!userRepo.isAdmin(userId)) {
            logger.error("User " + userId + " is not an admin");
            throw new RuntimeException("User is not an admin");
        }
        logger.info("Admin " + userId + " authenticated successfully");
        return userId;
    }
}
