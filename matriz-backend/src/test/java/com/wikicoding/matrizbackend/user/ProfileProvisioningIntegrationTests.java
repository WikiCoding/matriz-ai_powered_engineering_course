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

    @Test
    void deletionPreservesProjectsAndCommentsButRemovesAssignments() {
        // Arrange
        UUID team = UUID.randomUUID(), project = UUID.randomUUID(), task = UUID.randomUUID();
        UUID comment = UUID.randomUUID();
        jdbc.update("INSERT INTO team VALUES (?, 'Deletion test')", team);
        var user = profiles.create(UUID.randomUUID().toString(), UUID.randomUUID() + "@example.test", team);
        jdbc.update("INSERT INTO project (id, name, description, team_id, owner_id) VALUES (?, 'Project', '', ?, ?)",
                project, team, user.id());
        jdbc.update("INSERT INTO task (id, project_id, title, description, effort, status, priority, created_at, updated_at) "
                + "VALUES (?, ?, 'Task', '', 1, 'TODO', 'MEDIUM', now(), now())", task, project);
        jdbc.update("INSERT INTO task_assignment (task_id, user_id) VALUES (?, ?)", task, user.id());
        jdbc.update("INSERT INTO comment (id, task_id, author_id, content, created_at) "
                + "VALUES (?, ?, ?, 'Comment', now())", comment, task, user.id());

        // Act
        management.deleteLocal(user.id());

        // Assert
        assertThat(jdbc.queryForObject("SELECT deleted FROM app_user WHERE id = ?", Boolean.class, user.id()))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT team_id FROM app_user WHERE id = ?", UUID.class, user.id()))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM task_assignment WHERE user_id = ?", Long.class, user.id()))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT owner_id FROM project WHERE id = ?", UUID.class, project))
                .isEqualTo(user.id());
        assertThat(jdbc.queryForObject("SELECT author_id FROM comment WHERE id = ?", UUID.class, comment))
                .isEqualTo(user.id());
    }
}
