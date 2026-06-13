package com.ticketpurchasingsystem.project.init;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ticketpurchasingsystem.project.application.ActiveOrderService;
import com.ticketpurchasingsystem.project.application.EventService;
import com.ticketpurchasingsystem.project.application.HistoryOrderService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.UserService.UserService;
import com.ticketpurchasingsystem.project.domain.authentication.SessionToken;
import com.ticketpurchasingsystem.project.domain.Production.ManagerPermission;
import com.ticketpurchasingsystem.project.domain.User.UserGroupDiscount;
import com.ticketpurchasingsystem.project.domain.Utils.EventDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.Utils.PurchasePolicyDTO;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingAreaConfig;
import com.ticketpurchasingsystem.project.domain.event.Maps.SeatingMap;

// jsut glorify cases specific to the init file, not meant for general use.
// it is basically a big switch statement that calls the relevant service method based on the command name and arguments. 
// it also handles variable assignment and resolution for commands that return values
//  (like guest-entry or create-production-company).
// maybe there is more elegant way to do this. IDK
// if one exception is thrown, the whole initialization will fail and stop, which is probably what we want(they want they ask for it).
public class InitCommandExecutor {

    private final UserService userService;
    private final ProductionService productionService;
    private final EventService eventService;
    private final HistoryOrderService historyOrderService;
    private final ActiveOrderService activeOrderService;

    // holds $varName -> value (String token or Integer companyId)
    private final Map<String, Object> context = new HashMap<>();

    public InitCommandExecutor(UserService userService,
                               ProductionService productionService,
                               EventService eventService,
                               HistoryOrderService historyOrderService,
                               ActiveOrderService activeOrderService) {
        this.userService = userService;
        this.productionService = productionService;
        this.eventService = eventService;
        this.historyOrderService = historyOrderService;
        this.activeOrderService = activeOrderService;
    }

    public void execute(ParsedCommand cmd) {
        List<String> args = resolveArgs(cmd.args());
        Object result = dispatch(cmd.name(), args);

        if (cmd.hasVar() && result != null) {
            context.put(cmd.varName(), result);
        }
    }

    private List<String> resolveArgs(List<String> raw) {
        List<String> resolved = new ArrayList<>();
        for (String arg : raw) {
            if (arg.startsWith("$")) {
                String key = arg.substring(1);
                Object val = context.get(key);
                if (val == null) {
                    throw new RuntimeException("Undefined variable: " + arg);
                }
                resolved.add(String.valueOf(val));
            } else {
                resolved.add(arg);
            }
        }
        return resolved;
    }

    private Object dispatch(String name, List<String> args) {
        return switch (name) {

            // ── User commands ─────────────────────────────────────────────

            case "guest-entry" -> {
                // guest-entry()
                yield userService.guestEntry();
            }

            case "register" -> {
                // register(guestToken, userId, name, password, email, groupDiscount)
                userService.registerUser(
                        arg(args, 1), arg(args, 2), arg(args, 3),
                        arg(args, 4), UserGroupDiscount.valueOf(arg(args, 5)), arg(args, 0));
                yield null;
            }

            case "login" -> {
                // login(guestToken, userId, password)  →  returns token
                yield userService.loginUser(arg(args, 1), arg(args, 2), arg(args, 0));
            }

            case "admin-login" -> {
                // admin-login(guestToken, email, password)  →  returns token
                yield userService.loginAdmin(arg(args, 1), arg(args, 2), arg(args, 0));
            }

            case "logout" -> {
                // logout(userId, token)
                userService.logoutUser(arg(args, 0), arg(args, 1));
                yield null;
            }

            case "edit-username" -> {
                // edit-username(token, userId, oldName, newName)
                userService.editUsername(arg(args, 1), arg(args, 2), arg(args, 3), arg(args, 0));
                yield null;
            }

            case "edit-password" -> {
                // edit-password(token, userId, oldPassword, newPassword)
                userService.editPassword(arg(args, 1), arg(args, 2), arg(args, 3), arg(args, 0));
                yield null;
            }

            case "edit-email" -> {
                // edit-email(token, userId, oldEmail, newEmail)
                userService.editEmail(arg(args, 1), arg(args, 2), arg(args, 3), arg(args, 0));
                yield null;
            }

            case "set-group-discount" -> {
                // set-group-discount(token, userId, discount)
                userService.setUserGroupDiscount(
                        arg(args, 1), UserGroupDiscount.valueOf(arg(args, 2)), arg(args, 0));
                yield null;
            }

            // ── Production commands ───────────────────────────────────────

            case "create-production-company" -> {
                // create-production-company(token, name, description, contact)  →  returns companyId
                yield productionService.createProductionCompany(arg(args, 0),
                        new ProductionCompanyDTO(arg(args, 1), arg(args, 2), arg(args, 3)));
            }

            case "assign-owner" -> {
                // assign-owner(token, companyId, userId)
                productionService.assignOwner(
                        arg(args, 0), Integer.parseInt(arg(args, 1)), arg(args, 2));
                yield null;
            }

            case "appoint-manager" -> {
                // appoint-manager(token, companyId, managerId, perm1, perm2, ...)
                // permissions are optional; omitting them grants no permissions
                Set<ManagerPermission> perms = EnumSet.noneOf(ManagerPermission.class);
                for (int i = 3; i < args.size(); i++) {
                    perms.add(ManagerPermission.valueOf(arg(args, i)));
                }
                productionService.appointManager(
                        arg(args, 0), Integer.parseInt(arg(args, 1)), arg(args, 2), perms);
                yield null;
            }

            case "remove-manager" -> {
                // remove-manager(token, companyId, managerId)
                productionService.removeManager(
                        arg(args, 0), Integer.parseInt(arg(args, 1)), arg(args, 2));
                yield null;
            }

            case "remove-owner" -> {
                // remove-owner(token, companyId, ownerId)
                productionService.removeOwner(
                        arg(args, 0), Integer.parseInt(arg(args, 1)), arg(args, 2));
                yield null;
            }

            // ── Event commands ────────────────────────────────────────────

            case "create-event" -> {
                // create-event(token, eventId, companyId, name, capacity, date, hasSeats, location, price)
                eventService.createEvent(arg(args, 0),
                        new EventDTO(arg(args, 1), Integer.parseInt(arg(args, 2)),
                                arg(args, 3), Integer.parseInt(arg(args, 4)),
                                LocalDateTime.parse(arg(args, 5)),
                                Boolean.parseBoolean(arg(args, 6)), arg(args, 7),
                                Double.parseDouble(arg(args, 8))),
                        new PurchasePolicyDTO(null, null, false, null, null, false, false),
                        List.of());
                yield null;
            }

            case "remove-event" -> {
                // remove-event(token, eventId)
                eventService.removeEvent(arg(args, 0), arg(args, 1));
                yield null;
            }

            case "edit-event-price" -> {
                // edit-event-price(token, eventId, price)
                eventService.editEventPrice(arg(args, 0), arg(args, 1), Double.parseDouble(arg(args, 2)));
                yield null;
            }

            case "edit-event-date" -> {
                // edit-event-date(token, eventId, date)
                eventService.editEventDate(arg(args, 0), arg(args, 1), LocalDateTime.parse(arg(args, 2)));
                yield null;
            }

            case "edit-event-location" -> {
                // edit-event-location(token, eventId, location)
                eventService.editEventLocation(arg(args, 0), arg(args, 1), arg(args, 2));
                yield null;
            }

            case "edit-event-inventory" -> {
                // edit-event-inventory(token, eventId, newCapacity)
                eventService.editEventInventory(arg(args, 0), arg(args, 1), Integer.parseInt(arg(args, 2)));
                yield null;
            }

            case "configure-event-seating-map" -> {
                // configure-event-seating-map(token, eventId, rows1, seatsPerRow1, price1, rows2, ...)
                // seating areas are variadic triplets starting at index 2
                String token   = arg(args, 0);
                String eventId = arg(args, 1);

                if ((args.size() - 2) % 3 != 0 || args.size() < 5) {
                    throw new RuntimeException(
                            "configure-event-seating-map requires at least one seating area as (rows, seatsPerRow, price) triplets");
                }

                List<SeatingAreaConfig> seatingAreas = new ArrayList<>();
                for (int i = 2; i < args.size(); i += 3) {
                    int rows        = Integer.parseInt(arg(args, i));
                    int seatsPerRow = Integer.parseInt(arg(args, i + 1));
                    double price    = Double.parseDouble(arg(args, i + 2));
                    seatingAreas.add(new SeatingAreaConfig(rows, seatsPerRow, price));
                }

                SeatingMap seatingMap = eventService.configureSeatingMap(token, seatingAreas, List.of());
                eventService.editEventSeatingMap(token, eventId, seatingMap);
                yield null;
            }

            // ── Order commands ────────────────────────────────────────────

            case "create-active-order" -> {
                // create-active-order(token, userId, eventId)
                SessionToken token = new SessionToken(arg(args, 0), Long.MAX_VALUE);
                yield activeOrderService.createPendingOrder(token, arg(args, 1), arg(args, 2)).getOrderId();
            }

            case "add-seats-to-order" -> {
                // add-seats-to-order(token, orderId, seat1, seat2, ...)
                SessionToken token = new SessionToken(arg(args, 0), Long.MAX_VALUE);
                activeOrderService.addSeatsToActiveOrder(token, arg(args, 1), args.subList(2, args.size()));
                yield null;
            }

            case "create-history-order" -> {
                // create-history-order(orderId, userId, eventId, companyId, price, seat1, seat2, ...)
                historyOrderService.createHistoryOrder(
                        arg(args, 0), arg(args, 1), arg(args, 2),
                        Integer.parseInt(arg(args, 3)),
                        new Timestamp(System.currentTimeMillis()),
                        Double.parseDouble(arg(args, 4)),
                        args.subList(5, args.size()),
                        new HashMap<>());
                yield null;
            }

            default -> throw new RuntimeException("Unknown command: " + name);
        };
    }

    private String arg(List<String> args, int index) {
        if (index >= args.size()) {
            throw new RuntimeException("Missing argument at position " + index);
        }
        return args.get(index);
    }
}
