package com.ticketpurchasingsystem.project.domain.ProductionTest;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.Mock;
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
import com.ticketpurchasingsystem.project.domain.Utils.ProductionCompanyDTO;
import com.ticketpurchasingsystem.project.infrastructure.ProdRepo;

@ExtendWith(MockitoExtension.class)
public class CreateProductionCompanyTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private IProdRepo prodRepo;
    @Mock
    private ProductionEventPublisher productionEventPublisher;

    private ProductionService productionService;
    private ProductionHandler productionHandler;

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String USER_ID = "eden-42";

    @BeforeEach
    void setUp() {
        productionHandler = new ProductionHandler();
        productionService = new ProductionService(
                authenticationService, productionHandler, prodRepo, productionEventPublisher);
    }

    private ProductionCompanyDTO validDTO() {
        return new ProductionCompanyDTO("Awesome Events", "Great events company", "contact@awesome.com");
    }

    @Test
    public void GivenValidTokenAndCompanyDetails_WhenCreateProductionCompany_ThenReturnTrue() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenAnswer(inv -> {
            ProductionCompany c = inv.getArgument(0);
            c.setCompanyId(1);
            return c;
        });

        // Act
        Integer result = productionService.createProductionCompany(VALID_TOKEN, validDTO());

        // Assert
        assertNotNull(result);
    }

    @Test
    public void GivenInvalidToken_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(INVALID_TOKEN)).thenReturn(false);

        // Act
        Integer result = productionService.createProductionCompany(INVALID_TOKEN, validDTO());

        // Assert
        assertNull(result);
        verifyNoInteractions(prodRepo);
    }

    @Test
    public void GivenValidToken_WhenCreateProductionCompany_ThenUserIdIsResolvedFromToken() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionService.createProductionCompany(VALID_TOKEN, validDTO());

        // Assert
        verify(authenticationService).getUser(VALID_TOKEN);
        verify(prodRepo).save(argThat(c -> USER_ID.equals(c.getFounderId())));
    }

    @Test
    public void GivenCompanyNameAlreadyExists_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        ProductionCompanyDTO dto = validDTO();
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(dto.getCompanyName()))
                .thenReturn(Optional.of(new ProductionCompany(dto)));

        // Act
        Integer result = productionService.createProductionCompany(VALID_TOKEN, dto);

        // Assert
        assertNull(result);
        verify(prodRepo, never()).save(any());
    }

    @Test
    public void GivenRepoThrowsException_WhenCreateProductionCompany_ThenReturnFalse() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenThrow(new RuntimeException("DB error"));

        // Act
        Integer result = productionService.createProductionCompany(VALID_TOKEN, validDTO());

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenValidInput_WhenCreateProductionCompany_ThenRepoSaveIsCalled() {
        // Arrange
        when(authenticationService.validate(VALID_TOKEN)).thenReturn(true);
        when(authenticationService.getUser(VALID_TOKEN)).thenReturn(USER_ID);
        when(prodRepo.findByName(any())).thenReturn(Optional.empty());
        when(prodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productionService.createProductionCompany(VALID_TOKEN, validDTO());

        // Assert
        verify(prodRepo, times(1)).save(any());
    }

    @Test
    public void GivenBlankCompanyName_WhenCreateProductionCompany_ThenReturnNull() {
        // Arrange
        ProductionCompanyDTO dto = new ProductionCompanyDTO("   ", "desc", "email@test.com");

        // Act
        ProductionCompany result = productionHandler.createProductionCompany(USER_ID, dto);

        // Assert
        assertNull(result);
    }

    @Test
    public void GivenTwoDifferentFounders_WhenEachCreateOwnCompany_ThenEachCompanyHasCorrectFounder() {
        // Arrange
        String founderA = "founder-eden";
        String founderB = "founder-itay";
        ProductionCompanyDTO dtoA = new ProductionCompanyDTO("Eden Corp", "Eden's company", "eden@corp.com");
        ProductionCompanyDTO dtoB = new ProductionCompanyDTO("Itay Corp", "Itay's company", "itay@corp.com");

        // Act
        ProductionCompany companyA = productionHandler.createProductionCompany(founderA, dtoA);
        ProductionCompany companyB = productionHandler.createProductionCompany(founderB, dtoB);

        // Assert
        assertNotNull(companyA);
        assertNotNull(companyB);
        assertEquals(founderA, companyA.getFounderId());
        assertEquals(founderB, companyB.getFounderId());
    }

    @Test
    public void GivenFounderInitialisedTwice_WhenInitFounder_ThenFounderNotDuplicatedInOwners() {
        // Arrange
        ProductionCompany company = new ProductionCompany(validDTO());

        // Act
        company.initFounder(USER_ID);
        company.initFounder(USER_ID); // called a second time

        // Assert
        assertEquals(1, company.getOwnerIds().stream()
                .filter(id -> id.equals(USER_ID)).count(),
                "Founder ID must not be duplicated in ownerIds");
    }

    @Test
    public void GivenCompanyWithFounder_WhenAddOwner_ThenBothFounderAndOwnerAreInOwnerIds() {
        // Arrange
        ProductionCompany company = new ProductionCompany(validDTO());
        company.initFounder("founder-eden");

        // Act
        company.addOwnerId("owner-itay");

        // Assert
        assertTrue(company.getOwnerIds().contains("founder-eden"), "Founder must still be in ownerIds");
        assertTrue(company.getOwnerIds().contains("owner-itay"), "New owner must be in ownerIds");
        assertEquals(2, company.getOwnerIds().size());
    }

    // Concurrency tests

    private static final String CONC_SECRET = "my-super-secret-key-for-testing!";

    @Test
    public void GivenMultipleThreads_WhenConcurrentCreateDifferentCompanies_ThenAllSucceed() throws Exception {
        // Arrange
        com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo sessionRepo = new com.ticketpurchasingsystem.project.infrastructure.InMemorySessionRepo.InMemorySessionRepo();
        com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService domainAuth = new com.ticketpurchasingsystem.project.domain.authentication.DomainAuthService(
                sessionRepo);
        ReflectionTestUtils.setField(domainAuth, "secret", CONC_SECRET);
        domainAuth.init();
        com.ticketpurchasingsystem.project.application.AuthenticationService realAuth = new com.ticketpurchasingsystem.project.application.AuthenticationService(
                domainAuth, mock(SystemAdminService.class), sessionRepo);
        ProdRepo realRepo = new ProdRepo();
        ProductionService realService = new ProductionService(realAuth, new ProductionHandler(), realRepo,
                productionEventPublisher);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String token = realAuth.login("founder-" + idx);
                    Integer result = realService.createProductionCompany(token,
                            new ProductionCompanyDTO("Company-" + idx, "desc", "c" + idx + "@co.com"));
                    if (result != null)
                        successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
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
        assertEquals(0, errorCount.get(), "No exceptions should occur during concurrent company creation");
        assertEquals(threadCount, successCount.get(), "Each thread must create its own company successfully");
    }
}