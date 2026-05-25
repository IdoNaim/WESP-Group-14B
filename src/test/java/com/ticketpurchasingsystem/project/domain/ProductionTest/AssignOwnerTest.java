package com.ticketpurchasingsystem.project.domain.ProductionTest;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketpurchasingsystem.project.application.AuthenticationService;
import com.ticketpurchasingsystem.project.application.ProductionService;
import com.ticketpurchasingsystem.project.application.SystemAdminService;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.Production.ProductionEventPublisher;
import com.ticketpurchasingsystem.project.domain.Production.ProductionHandler;
import com.ticketpurchasingsystem.project.domain.Utils.OwnerDTO;
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService;
import com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

@ExtendWith(MockitoExtension.class)
public class AssignOwnerTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;
    @Mock
    private ProductionEventPublisher productionEventPublisher;
    @Captor
    private ArgumentCaptor<ProductionCompany> captor;

    private ProductionHandler productionHandler;
    private ProductionService productionService;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "bad-token";
    private static final String FOUNDER_ID = "eden-1";
    private static final String OWNER_ID = "itay-1";
    private static final String APPOINTEE_ID = "tomer-99";
    private static final Integer COMPANY_ID = 1;

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler();
        productionService = new ProductionService(
                authenticationService, productionHandler, prodRepo, productionEventPublisher);
    }

    private ProductionCompany companyWithFounderAndOwner() {
        ProductionCompanyDTO dto = new ProductionCompanyDTO("Test Co", "desc", "test@co.com");
        ProductionCompany company = new ProductionCompany(dto);
        company.setCompanyId(COMPANY_ID);
        company.initFounder(FOUNDER_ID);
        company.appointOwner(FOUNDER_ID, OWNER_ID);
        return company;
    }

    @Test
    public void GivenValidTokenAndValidAppointee_WhenAssignOwner_ThenReturnTrue() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    public void GivenInvalidToken_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        boolean result = productionService.assignOwner(INVALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo, productionEventPublisher);
    }

    @Test
    public void GivenAppointeeNotRegisteredUser_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(false);

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verifyNoInteractions(prodRepo);
    }

    @Test
    public void GivenValidToken_WhenAssignOwner_ThenCallerIdIsResolvedFromToken() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        verify(authenticationService).getUser(VALID_TOKEN);
    }

    @Test
    public void GivenCompanyDoesNotExist_WhenAssignOwner_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.empty());

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenValidInput_WhenAssignOwner_ThenRepoSaveIsCalledWithUpdatedCompany() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(FOUNDER_ID);
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(prodRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertTrue(result);
        verify(prodRepo, times(1)).save(any());
        assertTrue(captor.getValue().isOwner(APPOINTEE_ID),
                "Repo must receive the company with the new owner already added");
    }

    @Test
    public void GivenFailedAssignment_WhenAssignOwner_ThenRepoSaveIsNeverCalled() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn("not-an-owner");
        when(productionEventPublisher.publishIsUserRegisteredEvent(APPOINTEE_ID)).thenReturn(true);
        when(prodRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Act
        boolean result = productionService.assignOwner(VALID_TOKEN, COMPANY_ID, APPOINTEE_ID);

        // Assert
        assertFalse(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenFounderAsAppointerId_WhenAssignOwner_ThenReturnNonNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                FOUNDER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void GivenExistingOwnerAsAppointerId_WhenAssignOwner_ThenReturnNonNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                OWNER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void GivenCallerNotOwnerOfCompany_WhenAssignOwner_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                "random-user", COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenAppointeeAlreadyOwner_WhenAssignOwner_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                FOUNDER_ID, COMPANY_ID, OWNER_ID, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenNullAppointeeId_WhenAssignOwner_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                FOUNDER_ID, COMPANY_ID, null, company);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenFounderAppoints_WhenAssignOwner_ThenAppointeeHasFounderAsAppointer() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        productionHandler.assignOwner(FOUNDER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        OwnerDTO node = company.getOwnerDTO(APPOINTEE_ID).orElseThrow();
        assertEquals(FOUNDER_ID, node.getAppointerId());
    }

    @Test
    public void GivenOwnerAppoints_WhenAssignOwner_ThenAppointeeHasOwnerAsAppointer() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        productionHandler.assignOwner(OWNER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        OwnerDTO node = company.getOwnerDTO(APPOINTEE_ID).orElseThrow();
        assertEquals(OWNER_ID, node.getAppointerId());
    }

    @Test
    public void GivenSuccessfulAssignment_WhenAssignOwner_ThenReturnedCompanyContainsNewOwner() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                FOUNDER_ID, COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNotNull(result);
        assertTrue(result.isOwner(APPOINTEE_ID),
                "Returned company must contain the newly appointed owner");
    }

    @Test
    public void GivenFailedAssignment_WhenAssignOwner_ThenReturnNull() {
        // Arrange
        ProductionCompany company = companyWithFounderAndOwner();

        // Act
        ProductionCompany result = productionHandler.assignOwner(
                "not-an-owner", COMPANY_ID, APPOINTEE_ID, company);

        // Assert
        assertNull(result);
    }

    // Concurrency tests

    private static final String CONC_SECRET = "my-super-secret-key-for-testing!";

    private record ConcurrencyStack(AuthenticationService auth, ProdRepo repo, ProductionService service) {
    }

    private ConcurrencyStack buildRealStack() {
        InMemorySessionRepo sessionRepo = new InMemorySessionRepo();
        DomainAuthService domainAuth = new DomainAuthService(sessionRepo);
        ReflectionTestUtils.setField(domainAuth, "secret", CONC_SECRET);
        domainAuth.init();
        AuthenticationService realAuth = new AuthenticationService(domainAuth, sessionRepo);
        ProdRepo realRepo = new ProdRepo();
        ProductionService svc = new ProductionService(realAuth, new ProductionHandler(), realRepo,
                productionEventPublisher);
        return new ConcurrencyStack(realAuth, realRepo, svc);
    }

    @Test
    public void GivenMultipleThreads_WhenConcurrentAssignDifferentOwners_ThenAllSucceedThroughRetries()
            throws Exception {
        // Arrange
        lenient().when(productionEventPublisher.publishIsUserRegisteredEvent(any())).thenReturn(true);
        ConcurrencyStack stack = buildRealStack();
        String founderToken = stack.auth().login(FOUNDER_ID);
        stack.service().createProductionCompany(founderToken,
                new ProductionCompanyDTO("Shared Co", "desc", "shared@co.com"));
        int companyId = stack.repo().findByName("Shared Co").get().getCompanyId();

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String token = stack.auth().login(FOUNDER_ID);
                    boolean result = stack.service().assignOwner(token, companyId, "owner-" + idx);
                    if (result)
                        successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act
        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        // Assert
        assertEquals(threadCount, successCount.get(),
                "All concurrent assign-owner operations must eventually succeed through optimistic-locking retries");
    }

    @Test
    public void GivenMultipleThreads_WhenConcurrentAssignSameOwner_ThenOnlyOneSucceeds() throws Exception {
        // Arrange
        lenient().when(productionEventPublisher.publishIsUserRegisteredEvent(any())).thenReturn(true);
        ConcurrencyStack stack = buildRealStack();
        String founderToken = stack.auth().login(FOUNDER_ID);
        stack.service().createProductionCompany(founderToken,
                new ProductionCompanyDTO("Exclusive Co", "desc", "exclusive@co.com"));
        int companyId = stack.repo().findByName("Exclusive Co").get().getCompanyId();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String token = stack.auth().login(FOUNDER_ID);
                    boolean result = stack.service().assignOwner(token, companyId, "same-owner");
                    if (result)
                        successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Act
        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        // Assert
        assertEquals(1, successCount.get(),
                "Only one thread must succeed when all race to assign the same owner — duplicates must be rejected");
    }
}
