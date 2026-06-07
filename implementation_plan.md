# Secure Integration of BarCodeGateway and Payment Checkout Rollbacks

This plan details the architectural findings and steps required to securely integrate the external `BarCodeGateway` HTTP POST endpoint (https://damp-lynna-wsep-1984852e.koyeb.app/) and implement robust transaction rollback hooks in the ticket-purchasing checkout flow.

## Architectural Findings & Analysis

### 1. Code Search & Discovery (HTTP Adapter Configuration)
* **HTTP Request Library**: Tomer's HTTP integration in [PaymentGateway.java](file:///Users/maayandotan/Documents/תואר%20ראשון%20הנדסת%20תוכנה%20בן%20גוריון/שנה%20ג/סמסטר%20ב/WESP-Group-14B/src/main/java/com/ticketpurchasingsystem/project/infrastructure/PaymentGateway.java) utilizes the standard `RestTemplate` library.
* **REST Client Sharing Pattern**: A shared singleton `RestTemplate` bean is configured in [AppConfig.java](file:///Users/maayandotan/Documents/תואר%20ראשון%20הנדסת%20תוכנה%20בן%20גוריון/שנה%20ג/סמסטר%20ב/WESP-Group-14B/src/main/java/com/ticketpurchasingsystem/project/infrastructure/AppConfig.java) and injected into both `PaymentGateway` and `BarCodeGateway`.
* **Exception & Connection Settings**: 
  * The `RestTemplate` bean is instantiated as `new RestTemplate()` without any connection or read timeouts configured, which leaves request threads prone to infinite hangs in case of network latency or server cold starts on Koyeb.
  * In the existing gateway adapters, POST calls are executed directly. Any `RestClientException` throws directly up the stack, which in `ActiveOrderService` would bypass cleanup routines, leaving orders locked in a "processing" state forever.

### 2. Schema Alignment
* **Seat ID / Seating Structure**: 
  * **Assigned Seats**: Input strings follow the format `zone_row_seat` (split by `_`). These are mapped to a list containing a seating map: `seats: [{"row": row, "seat": seat}]` with `is_seating: true`.
  * **General Admission / Standing**: Parsed from `standingAreaQuantities` (a map of area ID strings to integer quantities). Each entry maps to a payload with `zone` (the area ID) and `quantity`.
* **Checkout Flow Sequence**:
  * Currently, barcodes are issued *before* capturing payment. If payment fails, the issued barcodes are cancelled.
  * To issue tickets *following* successful payment capture, the flow inside `ActiveOrderService.completeOrder` should be restructured:
    1. Capture payment first.
    2. If successful, issue barcodes.
    3. If barcode issuance fails, refund the captured payment.

### 3. System Rollback Analysis
* **Rollback Hooks**:
  * Reservation release is handled by `rollbackOrderReservations` in [ActiveOrderService.java](file:///Users/maayandotan/Documents/תואר%20ראשון%20הנדסת%20תוכנה%20בן%20גוריון/שנה%20ג/סמסטר%20ב/WESP-Group-14B/src/main/java/com/ticketpurchasingsystem/project/application/ActiveOrderService.java).
  * If the order fails after a successful payment capture but before complete persistence (due to exception in barcode generation, order publication, or repository deletion), we must catch all exceptions and perform a full rollback:
    * Refund the payment using `paymentGateway.refund(transactionId)`.
    * Cancel any issued tickets using `barCodeGateway.cancelTickets(barcodesIssued)`.
    * Release local reservations and delete the active order.

---

## Proposed Changes

### Component: Infrastructure Configuration

#### [MODIFY] [AppConfig.java](file:///Users/maayandotan/Documents/תואר%20ראשון%20הנדסת%20תוכנה%20בן%20גוריון/שנה%20ג/סמסטר%20ב/WESP-Group-14B/src/main/java/com/ticketpurchasingsystem/project/infrastructure/AppConfig.java)
- Update the `RestTemplate` bean configuration to set reasonable timeouts to prevent infinite blocking on remote HTTP calls:
  - Connection Timeout: 5000ms (5 seconds)
  - Read Timeout: 10000ms (10 seconds)
  - Configure timeouts via `SimpleClientHttpRequestFactory`.

---

### Component: External Gateways

#### [MODIFY] [BarCodeGateway.java](file:///Users/maayandotan/Documents/תואר%20ראשון%20הנדסת%20תוכנה%20בן%20גוריון/שנה%20ג/סמסטר%20ב/WESP-Group-14B/src/main/java/com/ticketpurchasingsystem/project/infrastructure/BarCodeGateway.java)
- Secure the POST request executing method:
  * Wrap external call in a try-catch block catching `Exception` to gracefully handle network issues.
  * Log exceptions using `loggerDef.getInstance()`.
  * Return `null` to indicate ticket generation failure, allowing caller to handle rollback and refund.

---

### Component: Checkout Application Service

#### [MODIFY] [ActiveOrderService.java](file:///Users/maayandotan/Documents/תואר%20ראשון%20הנדסת%20תוכנה%20בן%20גוריון/שנה%20ג/סמסטר%20ב/WESP-Group-14B/src/main/java/com/ticketpurchasingsystem/project/application/ActiveOrderService.java)
- Restructure `completeOrder` method:
  * Capture payment first via `payment(paymentGateway, ...)`.
  * Wrap the subsequent ticket-issuing and order persistence steps in a unified `try-catch` block.
  * If any exception is thrown, perform complete rollback (refund payment, cancel tickets if issued, release local reservations, and delete active order).

---

## Verification Plan

### Automated Tests
- Run `mvn clean test-compile` to verify compilation.
- Run `mvn test` to ensure existing and refactored tests pass.
- Write new unit tests in a dedicated `BarCodeGatewayTest.java` class:
  * Verify `issueBarcodes` returns expected barcodes on successful HTTP response.
  * Verify `issueBarcodes` returns `null` and cancels already issued barcodes if a later HTTP request fails or throws exception.
- Write unit tests in [ActiveOrderServiceUnitTest.java](file:///Users/maayandotan/Documents/תואר%20ראשון%20הנדסת%20תוכנה%20בן%20גוריון/שנה%20ג/סמסטר%20ב/WESP-Group-14B/src/test/java/com/ticketpurchasingsystem/project/domain/ActiveOrders/ActiveOrderServiceUnitTest.java):
  * Mock `IPaymentGateway` and `IBarCodeGateway`.
  * Assert that if payment fails, reservations are rolled back and order deleted.
  * Assert that if payment succeeds but barcode generation fails, refund is triggered, reservations are rolled back, and order deleted.
  * Assert that if database persistence fails after payment and barcodes are completed, refund and cancel tickets are executed.

### Manual Verification
- Perform manual verification by executing full order completions against the live endpoint `https://damp-lynna-wsep-1984852e.koyeb.app/` using temporary testing scripts.
