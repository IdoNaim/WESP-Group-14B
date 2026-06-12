# System Initialization File

The system loads an init file on startup that seeds it with users, companies, events, and orders.
All commands run through the application service layer — only legal operations are allowed.
If any command fails, the entire initialization fails and the application stops.

---

## Changing the init file

**Default:** `src/main/resources/init_db_file.txt` (loaded from classpath).

### Option 1 — application.properties
```properties
init.file=init_db_file.txt
```

### Option 2 — Command-line argument (overrides application.properties)
```bash
java -jar ticket.jar --init.file=init_db_file.txt
```
You can also point to a file outside the JAR using an absolute path:
```bash
java -jar ticket.jar --init.file=/data/my_init.txt
```

### Option 3 — Maven (dev/test runs)
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--init.file=my_other_file.txt"
```

### Disabling initialization
Leave the file empty or delete all non-comment lines. The loader skips blank lines and `#` comments.

---

## File syntax

```
# This is a comment
$varName = command(arg1, arg2, ...);
command(arg1, arg2, ...);
```

- Lines starting with `#` are ignored.
- `$varName = ...` stores the return value of a command (e.g. a token or company ID).
- `$varName` anywhere in an argument list resolves the stored value.
- Every line must end with `;`.

---

## Supported commands

### User commands

| Command | Arguments | Returns |
|---|---|---|
| `guest-entry()` | — | token |
| `register(guestToken, userId, name, password, email, discount)` | discount: `NONE`, `STUDENT`, `EMPLOYEE`, `MILITARY` | — |
| `login(guestToken, userId, password)` | — | token |
| `admin-login(guestToken, email, password)` | — | token |
| `logout(userId, token)` | — | — |
| `edit-username(token, userId, oldName, newName)` | — | — |
| `edit-password(token, userId, oldPassword, newPassword)` | — | — |
| `edit-email(token, userId, oldEmail, newEmail)` | — | — |
| `set-group-discount(token, userId, discount)` | — | — |

### Production commands

| Command | Arguments | Returns |
|---|---|---|
| `create-production-company(token, name, description, contact)` | — | companyId |
| `assign-owner(token, companyId, userId)` | — | — |
| `appoint-manager(token, companyId, managerId, perm1, perm2, ...)` | permissions optional | — |
| `remove-manager(token, companyId, managerId)` | — | — |
| `remove-owner(token, companyId, ownerId)` | — | — |

Available manager permissions:
`INVENTORY_MANAGEMENT`, `VENUE_CONFIGURATION_AND_EVENT_MAPPING`, `COMPANY_POLICY_MANAGEMENT`,
`PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT`, `CUSTOMER_INQUIRY_AND_RESPONSE_MANAGEMENT`,
`PURCHASE_AND_ORDER_HISTORY_ACCESS`, `SALES_REPORT_GENERATION`

### Event commands

| Command | Arguments | Returns |
|---|---|---|
| `create-event(token, eventId, companyId, name, capacity, date, hasSeats, location, price)` | date: `2026-07-11T20:00` | — |
| `configure-event-seating-map(token, eventId, rows, seatsPerRow, price, ...)` | repeating triplets for each section | — |
| `remove-event(token, eventId)` | — | — |
| `edit-event-price(token, eventId, price)` | — | — |
| `edit-event-date(token, eventId, date)` | date: `2026-07-11T20:00` | — |
| `edit-event-location(token, eventId, location)` | — | — |
| `edit-event-inventory(token, eventId, newCapacity)` | — | — |

### Order commands

| Command | Arguments | Returns |
|---|---|---|
| `create-history-order(orderId, userId, eventId, companyId, price, seat1, seat2, ...)` | seats are variadic | — |

---

## Full example

```
# Users
$guest1 = guest-entry();
register($guest1, alice, Alice Smith, pass123, alice@example.com, NONE);
$alice = login($guest1, alice, pass123);

$guest2 = guest-entry();
register($guest2, bob, Bob Jones, pass456, bob@example.com, STUDENT);
$bob = login($guest2, bob, pass456);

# Production
$company1 = create-production-company($alice, Live Events Co., Premier organizer, contact@live.com);
appoint-manager($alice, $company1, bob, INVENTORY_MANAGEMENT, SALES_REPORT_GENERATION);

# Events
create-event($alice, evt1, $company1, Rock Night, 500, 2026-07-11T20:00, true, Tel Aviv Arena, 120.0);
configure-event-seating-map($alice, evt1, 10, 10, 120.0, 10, 10, 90.0, 5, 10, 60.0);

# Orders
create-history-order(order1, bob, evt1, $company1, 120.0, A1, A2);

# Logout
logout(alice, $alice);
logout(bob, $bob);
```
