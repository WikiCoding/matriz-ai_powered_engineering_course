package com.wikicoding.matrizbackend.user;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ProfileProvisioningIntegrationTests {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.4");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired ProfileProvisioning profiles;
    @Autowired UserManagement management;
    @Autowired JdbcTemplate jdbc;

    @Test
    void localProfilePersistsIdentityAndExactlyOneTeam() {
        // Arrange
        UUID team = UUID.randomUUID();
        String subject = UUID.randomUUID().toString();
        String email = UUID.randomUUID() + "@example.test";
        jdbc.update("INSERT INTO team VALUES (?, 'Provisioning test')", team);
        // Act
        var profile = profiles.create(subject, email, team);
        // Assert
        var saved = jdbc.queryForMap("SELECT * FROM app_user WHERE id = ?", profile.id());
        assertThat(saved).containsEntry("keycloak_subject", subject).containsEntry("email", email)
                .containsEntry("team_id", team).containsEntry("provisioning_status", "COMPLETED");
    }

    @Test
    void duplicateEmailAndMissingTeamDoNotLeavePartialProfiles() {
        // Arrange
        UUID team = UUID.randomUUID();
        String email = UUID.randomUUID() + "@example.test";
        jdbc.update("INSERT INTO team VALUES (?, 'Provisioning test')", team);
        var existing = profiles.create(UUID.randomUUID().toString(), email, team);
        // Act / Assert: rejected transactions leave the existing profile unchanged.
        assertThatThrownBy(() -> profiles.create(UUID.randomUUID().toString(), email.toUpperCase(), team))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> profiles.create(UUID.randomUUID().toString(), email + ".other", UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE team_id = ?", Long.class, team)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT email FROM app_user WHERE id = ?", String.class, existing.id())).isEqualTo(email);
    }

}
