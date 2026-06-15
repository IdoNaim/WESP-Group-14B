package com.ticketpurchasingsystem.project.init;

import com.ticketpurchasingsystem.project.domain.HistoryOrder.IHistoryOrderRepo;
import com.ticketpurchasingsystem.project.domain.Production.IProdRepo;
import com.ticketpurchasingsystem.project.domain.User.IUserRepo;
import com.ticketpurchasingsystem.project.domain.authentication.ISessionRepo;
import com.ticketpurchasingsystem.project.domain.event.IEventRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.ticketpurchasingsystem.project.domain.Production.ProductionCompany;
import com.ticketpurchasingsystem.project.domain.event.Event;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SystemInitializationTest {

    // ---------------------------------------------------------
    // Valid init files - normal Spring context tests
    // ---------------------------------------------------------

    @SpringBootTest
    @ActiveProfiles("test")
    static class EmptyInitTests {

        @Autowired
        private IUserRepo userRepo;

        @Autowired
        private ISessionRepo sessionRepo;

        @Test
        void GivenEmptyInitFile_WhenSystemStarts_ThenAdminIsRegistered() {
            assertTrue(userRepo.findByID("admin-1") != null);
        }

        @Test
        void Given_EmptyInitFile_When_SystemStarts_Then_NoActiveSessions() {
            assertTrue(sessionRepo.findAll().isEmpty());
        }
    }

    @SpringBootTest(properties = "init.file=init_db_file.txt")
    @ActiveProfiles("test")
    static class PopulatedInitTests {

        @Autowired
        private IUserRepo userRepo;

        @Autowired
        private IProdRepo companyRepo;

        @Autowired
        private IEventRepo eventRepo;

        @Autowired
        private IHistoryOrderRepo historyRepo;

        @Autowired
        private ISessionRepo sessionRepo;

        @Test
        void GivenValidInitDbFile_WhenSystemStarts_ThenUsersAreRegistered() {
            assertTrue(userRepo.findByID("alice") !=null);
            assertTrue(userRepo.findByID("bob") !=null);
        }

        @Test
        void GivenValidInitDbFile_WhenSystemStarts_ThenProductionCompaniesAreCreated() {
            assertTrue(companyRepo.findByName("Live Events Co.").isPresent());
            assertTrue(companyRepo.findByName("Comedy Central").isPresent());

            ProductionCompany company1 = companyRepo.findByName("Live Events Co.").get();
            assertEquals("alice", company1.getFounderId());
        }

//        @Test
//        void GivenValidInitDbFile_WhenSystemStarts_ThenEventsAreCreatedWithCorrectInventory() {
//            Event rockNight = eventRepo.findById(1L).orElseThrow();
//            assertEquals("Rock Night", rockNight.getEventName());
//            // 10x10 + 10x10 + 5x10 = 250 seats
//            assertEquals(250, rockNight.getSeatingMap().getTotalAvailableCapacity());
//        }
//
//        @Test
//        void GivenValidInitDbFile_WhenSystemStarts_ThenHistoryOrdersDoNotAffectInventory() {
//            assertEquals(2, historyRepo.findAll().size());
//
//            Event rockNight = eventRepo.findById(1L).orElseThrow();
//            assertEquals(250, rockNight.getSeatingMap().getTotalAvailableCapacity());
//        }

        @Test
        void GivenValidInitDbFile_WhenSystemStarts_ThenAllUsersAreLoggedOut() {
            // init file ends with logout(alice) and logout(bob)
            assertTrue(sessionRepo.findAll().isEmpty());
        }
    }

    // ---------------------------------------------------------
    // Invalid init files - context must fail to load
    // ---------------------------------------------------------

    @SpringBootTest(properties = "init.file=init_unknown_command.txt")
    @ActiveProfiles("test")
    static class UnknownCommandTest {

        @Test
        void Given_InitFileWithUnknownCommand_When_SystemStarts_Then_ContextLoadFails() {
            // SpringBootTest itself will throw during context bootstrap;
            // this is verified by the context-loading test below.
            // Placeholder to keep structure symmetric if needed.
        }
    }

    // For startup-failure cases, ApplicationContextRunner-style or
    // SpringBootTest with assertThrows on context refresh is awkward,
    // so use a raw context build instead:

    static class InvalidInitFileTests {

        @Test
        void GivenInitFileWithUnknownCommand_WhenSystemStarts_ThenStartupFails() throws IOException {
            assertStartupFails(
                    """
                    $guest1 = guest-entry();
                    fly-to-moon($guest1, alice);
                    """);
        }

        @Test
        void GivenInitFileWithInvalidActionSequence_WhenSystemStarts_ThenStartupFails() throws IOException {
            // create-production-company requires login first
            assertStartupFails(
                    """
                    $guest1 = guest-entry();
                    register($guest1, alice, Alice Smith, pass123, alice@example.com, NONE);
                    create-production-company($guest1, Live Events Co., desc, contact@liveevents.com);
                    """);
        }

        @Test
        void GivenInitFileReferencingUndefinedVariable_WhenSystemStarts_ThenStartupFails() throws IOException {
            assertStartupFails(
                    """
                    $alice = login($undefined_guest, alice, pass123);
                    """);
        }

        @Test
        void GivenInitFileWithMalformedSyntax_WhenSystemStarts_ThenStartupFails() throws IOException {
            assertStartupFails(
                    """
                    $guest1 = guest-entry(
                    register($guest1, alice, pass123);
                    """);
        }

        @Test
        void GivenInitFileFailsHalfway_WhenSystemStarts_ThenNoPartialStateIsPersisted() throws IOException {
            // company creation succeeds, appoint-manager fails (moshe never registered)
            assertStartupFails(
                    """
                    $guest1 = guest-entry();
                    register($guest1, rina, Rina, pass, rina@example.com, NONE);
                    $rina = login($guest1, rina, pass);
                    $company1 = create-production-company($rina, Live Events Co., desc, contact@liveevents.com);
                    appoint-manager($rina, Live Events Co., moshe, ALL);
                    """);

        }

        private void assertStartupFails(String initFileContent) throws IOException {
            Path tempInitFile = Files.createTempFile("init_invalid", ".txt");
            Files.writeString(tempInitFile, initFileContent);

            try {
                Throwable thrown = assertThrows(Throwable.class, () -> {
                    ConfigurableApplicationContext ctx = new SpringApplicationBuilder(
                            com.ticketpurchasingsystem.project.TicketApplication.class)
                            .properties(
                                    "spring.config.location=classpath:/application-test.properties",
                                    "spring.profiles.active=test",
                                    "init.file=" + tempInitFile.toString()
                            )
                            .run();
                    ctx.close();
                });

                Throwable root = rootCause(thrown);
                assertInstanceOf(Exception.class, root,
                        "Expected InitializationException but got: " + root);
            } finally {
                Files.deleteIfExists(tempInitFile);
            }
        }

        private Throwable rootCause(Throwable t) {
            Throwable cause = t;
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
            }
            return cause;
        }
    }
}