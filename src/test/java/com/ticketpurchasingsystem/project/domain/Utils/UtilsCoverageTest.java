package com.ticketpurchasingsystem.project.domain.Utils;

import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsCoverageTest {

    @Test
    public void testRolesTreeDTOToStringAndGetters() {
        OwnerDTO founder = new OwnerDTO("owner1", null);
        OwnerDTO owner2 = new OwnerDTO("owner2", "owner1");

        Set<ManagerPermission> perms = Set.of(ManagerPermission.COMPANY_POLICY_MANAGEMENT);
        ManagerDTO manager = new ManagerDTO("manager1", "owner1", perms);
        ManagerDTO manager2 = new ManagerDTO("manager2", "owner2", Set.of());

        Map<String, OwnerDTO> ownershipTree = new HashMap<>();
        ownershipTree.put("owner1", founder);
        ownershipTree.put("owner2", owner2);

        Map<String, ManagerDTO> managerTree = new HashMap<>();
        managerTree.put("manager1", manager);
        managerTree.put("manager2", manager2);

        Map<String, Set<ManagerPermission>> managerPermissions = new HashMap<>();
        managerPermissions.put("manager1", perms);

        RolesTreeDTO dto = new RolesTreeDTO(
                100,
                null,
                "owner1",
                ownershipTree,
                managerTree,
                managerPermissions
        );

        assertEquals(100, dto.getCompanyId());
        assertEquals("owner1", dto.getFounderId());
        assertEquals(ownershipTree, dto.getOwnershipTree());
        assertEquals(managerTree, dto.getManagerTree());
        assertEquals(managerPermissions, dto.getManagerPermissions());

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("company : 100"));
        assertTrue(str.contains("founder : owner1"));
        assertTrue(str.contains("owner1"));
        assertTrue(str.contains("owner2"));
        assertTrue(str.contains("manager1"));
        assertTrue(str.contains("manager2"));
    }

    @Test
    public void testOwnerDTOBehavior() {
        OwnerDTO founder = new OwnerDTO("owner1", null);
        OwnerDTO appointed = new OwnerDTO("owner2", "owner1");

        assertEquals("owner1", founder.getUserId());
        assertNull(founder.getAppointerId());
        assertTrue(founder.isFounder());

        assertEquals("owner2", appointed.getUserId());
        assertEquals("owner1", appointed.getAppointerId());
        assertFalse(appointed.isFounder());

        String str = founder.toString();
        assertTrue(str.contains("OwnerDTO{userId='owner1', appointerId='null'}"));
    }

    @Test
    public void testManagerDTOBehavior() {
        // Branch 1: empty permissions
        ManagerDTO managerEmpty = new ManagerDTO("manager1", "owner1", Collections.emptySet());
        assertEquals("manager1", managerEmpty.getUserId());
        assertEquals("owner1", managerEmpty.getAppointerId());
        assertTrue(managerEmpty.getPermissions().isEmpty());

        // Branch 2: non-empty permissions
        Set<ManagerPermission> perms = Set.of(ManagerPermission.COMPANY_POLICY_MANAGEMENT, ManagerPermission.INVENTORY_MANAGEMENT);
        ManagerDTO managerWithPerms = new ManagerDTO("manager2", "owner2", perms);
        assertEquals(perms, managerWithPerms.getPermissions());
        assertTrue(managerWithPerms.hasPermission(ManagerPermission.COMPANY_POLICY_MANAGEMENT));
        assertFalse(managerWithPerms.hasPermission(ManagerPermission.PURCHASE_AND_ORDER_HISTORY_ACCESS));

        String str = managerWithPerms.toString();
        assertTrue(str.contains("manager2"));
    }

    @Test
    public void testPasswordEncoderUtilAndIdGenerator() {
        // PasswordEncoderUtil
        String pw = "mySecretPassword123";
        String encoded = PasswordEncoderUtil.encodePassword(pw);
        assertNotNull(encoded);
        assertTrue(PasswordEncoderUtil.matches(pw, encoded));
        assertFalse(PasswordEncoderUtil.matches("wrongPassword", encoded));

        // IdGenerator public API
        IdGenerator generator = IdGenerator.getInstance();
        assertNotNull(generator);
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        assertEquals(id1 + 1, id2);

        // IdGenerator private constructor test for 100% coverage
        try {
            Constructor<IdGenerator> constructor = IdGenerator.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            IdGenerator newGenerator = constructor.newInstance();
            assertNotNull(newGenerator);
            assertEquals(1, newGenerator.nextId());
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            fail("Failed to invoke private constructor of IdGenerator: " + e.getMessage());
        }
    }

    @Test
    public void testAllDTOsAndRecords() {
        // DiscountDTO
        LocalDateTime validUntil = LocalDateTime.now();
        DiscountDTO discountDTO = new DiscountDTO(
                "VISIBLE", "Sale", 10.0, validUntil, "SAVE10", 3, 1
        );
        assertEquals("VISIBLE", discountDTO.type());
        assertEquals("Sale", discountDTO.name());
        assertEquals(10.0, discountDTO.percentage());
        assertEquals(validUntil, discountDTO.validUntil());
        assertEquals("SAVE10", discountDTO.couponCode());
        assertEquals(3, discountDTO.haveToBuyAmount());
        assertEquals(1, discountDTO.getForFreeAmount());

        // PurchasePolicyDTO
        PurchasePolicyDTO purchaseDTO = new PurchasePolicyDTO(
                2, 5, true, 18, 60, false, true
        );
        assertEquals(2, purchaseDTO.minTickets());
        assertEquals(5, purchaseDTO.maxTickets());
        assertTrue(purchaseDTO.isQuantityOr());
        assertEquals(18, purchaseDTO.minAge());
        assertEquals(60, purchaseDTO.maxAge());
        assertFalse(purchaseDTO.isAgeOr());
        assertTrue(purchaseDTO.isAgeAndQuantityOr());

        // EventDTO
        EventDTO eventDTO = new EventDTO(
                "event1", 99, "Concert", 5000, validUntil, true
        );
        assertEquals("event1", eventDTO.eventId());
        assertEquals(99, eventDTO.companyId());
        assertEquals("Concert", eventDTO.eventName());
        assertEquals(5000, eventDTO.eventCapacity());
        assertEquals(validUntil, eventDTO.eventDateTime());
        assertTrue(eventDTO.isActive());

        // NotificationDTO
        NotificationDTO notificationDTO = new NotificationDTO(
                "notif1", "user1", "Welcome", false, validUntil
        );
        assertEquals("notif1", notificationDTO.getId());
        assertEquals("user1", notificationDTO.getUserId());
        assertEquals("Welcome", notificationDTO.getMessage());
        assertFalse(notificationDTO.isRead());
        assertEquals(validUntil, notificationDTO.getCreatedAt());

        // HistoryOrderDTO
        Timestamp purchaseTime = Timestamp.valueOf(validUntil);
        List<String> seatIds = List.of("A1", "A2");
        HashMap<String, Integer> standingQuantities = new HashMap<>();
        standingQuantities.put("StandingArea1", 2);

        HistoryOrderDTO historyDTO = new HistoryOrderDTO(
                "order1", "user1", "event1", 123, purchaseTime, 299.9, seatIds, standingQuantities
        );
        assertEquals("order1", historyDTO.getOrderId());
        assertEquals("user1", historyDTO.getUserId());
        assertEquals("event1", historyDTO.getEventId());
        assertEquals(123, historyDTO.getCompanyId());
        assertEquals(purchaseTime, historyDTO.getPurchaseDate());
        assertEquals(299.9, historyDTO.getPrice());
        assertEquals(seatIds, historyDTO.getSeatIds());
        assertEquals(standingQuantities, historyDTO.getStandingAreaQuantities());

        // ProductionCompanyDTO
        ProductionCompanyDTO companyDTO = new ProductionCompanyDTO("Name", "Desc", "email@test.com");
        assertEquals("Name", companyDTO.getCompanyName());
        assertEquals("Desc", companyDTO.getCompanyDescription());
        assertEquals("email@test.com", companyDTO.getCompanyEmail());

        companyDTO.setCompanyName("NewName");
        companyDTO.setCompanyDescription("NewDesc");
        companyDTO.setCompanyEmail("newemail@test.com");
        assertEquals("NewName", companyDTO.getCompanyName());
        assertEquals("NewDesc", companyDTO.getCompanyDescription());
        assertEquals("newemail@test.com", companyDTO.getCompanyEmail());

        ProductionCompanyDTO companyDTO2 = new ProductionCompanyDTO();
        assertNotNull(companyDTO2);

        // Listener & Publisher
        Listener listener = new Listener();
        assertNotNull(listener);

        Publisher publisher = new Publisher();
        assertNotNull(publisher);
    }
}
