package com.ticketpurchasingsystem.project;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ConfigurationLoadingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TicketApplication.class);

    @Test
    void GivenMissingJwtSecret_WhenContextLoads_ThenInitializationFails() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=",
                        "spring.security.user.password=dummy-password-12345",
                        "spring.datasource.url=jdbc:h2:mem:testdb_missing_jwt;DB_CLOSE_DELAY=-1",
                        "init.file=empty_init.txt"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                });
    }

    @Test
    void GivenMalformedJwtSecret_WhenContextLoads_ThenInitializationFails() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=short",
                        "spring.security.user.password=dummy-password-12345",
                        "spring.datasource.url=jdbc:h2:mem:testdb_short_jwt;DB_CLOSE_DELAY=-1",
                        "init.file=empty_init.txt"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                });
    }

    @Test
    void GivenMissingSecurityPassword_WhenContextLoads_ThenInitializationFails() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong",
                        "spring.security.user.password=",
                        "spring.datasource.url=jdbc:h2:mem:testdb_missing_pwd;DB_CLOSE_DELAY=-1",
                        "init.file=empty_init.txt"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                });
    }

    @Test
    void GivenValidConfiguration_WhenContextLoads_ThenInitializationSucceeds() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong",
                        "spring.security.user.password=valid-password-12345",
                        "spring.datasource.url=jdbc:h2:mem:testdb_valid;DB_CLOSE_DELAY=-1",
                        "init.file=empty_init.txt"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }
}
