package com.ticketpurchasingsystem.project;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import com.ticketpurchasingsystem.project.infrastructure.ConfigurationValidator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

class ConfigurationLoadingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TicketApplication.class);

    // Minimal context runner registering only a test configuration and our ConfigurationValidator.
    // This allows verifying property loading and basic validation without booting database resources or component scanning.
    private final ApplicationContextRunner minimalContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class, ConfigurationValidator.class);

    @Configuration
    static class TestConfig {
    }

    // Negative (failure) configuration tests
    @Test
    void GivenMissingJwtSecret_WhenContextLoads_ThenInitializationFails() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=",
                        "spring.security.user.name=user",
                        "spring.security.user.password=dummy-password-12345",
                        "spring.profiles.active=test",
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
                        "spring.security.user.name=user",
                        "spring.security.user.password=dummy-password-12345",
                        "spring.profiles.active=test",
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
                        "spring.security.user.name=user",
                        "spring.security.user.password=",
                        "spring.profiles.active=test",
                        "spring.datasource.url=jdbc:h2:mem:testdb_missing_pwd;DB_CLOSE_DELAY=-1",
                        "init.file=empty_init.txt"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                });
    }

    @Test
    void GivenMissingSecurityUsername_WhenContextLoads_ThenInitializationFails() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong",
                        "spring.security.user.name=",
                        "spring.security.user.password=dummy-password-12345",
                        "spring.profiles.active=test",
                        "spring.datasource.url=jdbc:h2:mem:testdb_missing_user;DB_CLOSE_DELAY=-1",
                        "init.file=empty_init.txt"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                });
    }

    @Test
    void GivenMissingActiveProfiles_WhenContextLoads_ThenInitializationFails() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong",
                        "spring.security.user.name=user",
                        "spring.security.user.password=dummy-password-12345",
                        "spring.profiles.active=",
                        "spring.datasource.url=jdbc:h2:mem:testdb_missing_profile;DB_CLOSE_DELAY=-1",
                        "init.file=empty_init.txt"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                });
    }

    // Positive (success) path configuration test
    @Test
    void GivenValidConfiguration_WhenContextLoads_ThenInitializationSucceeds() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong",
                        "spring.security.user.name=user",
                        "spring.security.user.password=valid-password-12345",
                        "spring.profiles.active=test",
                        "spring.datasource.url=jdbc:h2:mem:testdb_valid;DB_CLOSE_DELAY=-1",
                        "init.file=empty_init.txt",
                        "payment.gateway.api.url=https://damp-lynna-wsep-1984852e.koyeb.app/",
                        "barcode.gateway.api.url=https://damp-lynna-wsep-1984852e.koyeb.app/",
                        "admin.id=admin-1",
                        "admin.name=Admin",
                        "admin.email=admin@gmail.com",
                        "admin.password=admin123"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    private Properties loadPropertiesFile(String relativePath) throws IOException {
        Properties properties = new Properties();
        java.io.File file = new java.io.File(relativePath);
        if (!file.exists()) {
            java.io.File parent = new java.io.File(".").getAbsoluteFile();
            while (parent != null) {
                java.io.File candidate = new java.io.File(parent, relativePath);
                if (candidate.exists()) {
                    file = candidate;
                    break;
                }
                parent = parent.getParentFile();
            }
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        }
        return properties;
    }

    @Test
    void VerifyProductionProperties_WhenContextLoads_ThenInjectedFieldsAreCorrect() throws Exception {
        Properties mainProps = loadPropertiesFile("src/main/resources/application.properties");
        Properties prodProps = loadPropertiesFile("src/main/resources/application-prod.properties");

        Properties mergedProps = new Properties();
        mergedProps.putAll(mainProps);
        mergedProps.putAll(prodProps);
        mergedProps.setProperty("spring.profiles.active", "prod");

        String[] envProps = mergedProps.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);

        minimalContextRunner
                .withPropertyValues(envProps)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Environment env = context.getBean(Environment.class);
                    assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://35.192.79.142:5432/postgres");
                    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("postgres");
                    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("Pass_1234");
                    assertThat(env.getProperty("spring.security.user.name")).isEqualTo("user");
                    assertThat(env.getProperty("spring.security.user.password")).isEqualTo("409e2525-e72e-4d5c-bedd-2b9d7af70449");
                    assertThat(env.getProperty("jwt.secret")).isEqualTo("myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong");
                });
    }

    @Test
    void VerifyDevelopmentProperties_WhenContextLoads_ThenInjectedFieldsAreCorrect() throws Exception {
        Properties mainProps = loadPropertiesFile("src/main/resources/application.properties");
        Properties devProps = loadPropertiesFile("src/main/resources/application-dev.properties");

        Properties mergedProps = new Properties();
        mergedProps.putAll(mainProps);
        mergedProps.putAll(devProps);
        mergedProps.setProperty("spring.profiles.active", "dev");

        String[] envProps = mergedProps.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);

        minimalContextRunner
                .withPropertyValues(envProps)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Environment env = context.getBean(Environment.class);
                    assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:ticketdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
                    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("sa");
                    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("");
                    assertThat(env.getProperty("spring.security.user.name")).isEqualTo("user");
                    assertThat(env.getProperty("spring.security.user.password")).isEqualTo("409e2525-e72e-4d5c-bedd-2b9d7af70449");
                    assertThat(env.getProperty("jwt.secret")).isEqualTo("myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong");
                });
    }

    @Test
    void VerifyTestProperties_WhenContextLoads_ThenInjectedFieldsAreCorrect() throws Exception {
        Properties testBaseProps = loadPropertiesFile("src/test/resources/application.properties");
        Properties testProps = loadPropertiesFile("src/test/resources/application-test.properties");

        Properties mergedProps = new Properties();
        mergedProps.putAll(testBaseProps);
        mergedProps.putAll(testProps);
        mergedProps.setProperty("spring.profiles.active", "test");

        String[] envProps = mergedProps.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);

        minimalContextRunner
                .withPropertyValues(envProps)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Environment env = context.getBean(Environment.class);
                    assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
                    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("sa");
                    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("");
                    assertThat(env.getProperty("spring.security.user.name")).isEqualTo("user");
                    assertThat(env.getProperty("spring.security.user.password")).isEqualTo("409e2525-e72e-4d5c-bedd-2b9d7af70449");
                    assertThat(env.getProperty("jwt.secret")).isEqualTo("myUltraSecretKeyForJWTSigningThatIsAtLeast32CharactersLong");
                });
    }
}
