package com.wikicoding.matrizbackend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class MatrizBackendApplicationTests {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;

    @Test
    void startupMigratesEmptyDatabaseAndRepeatMigrationIsANoOp() {
        // Arrange
        UUID demoTeam = UUID.fromString("e0a987ab-bff0-4de8-b320-04b1f42899aa");

        // Act
        var applied = flyway.info().applied();
        var repeated = flyway.migrate();

        // Assert
        assertThat(applied).extracting(migration -> migration.getVersion().getVersion())
                .containsExactly("1");
        assertThat(flyway.info().pending()).isEmpty();
        flyway.validate();
        assertThat(repeated.migrationsExecuted).isZero();
        assertThat(count("team", "id", demoTeam)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE team_id = ? AND provisioning_status = 'COMPLETED' AND keycloak_subject = id::text", Integer.class, demoTeam)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT to_regclass('public.event_publication') IS NULL", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT to_regclass('public.audit_record') IS NULL", Boolean.class)).isTrue();
    }

    @Test
    void duplicateEmailsAreRejectedCaseInsensitively() {
        // Arrange
        UUID team = UUID.randomUUID(), user = UUID.randomUUID();
        jdbc.update("INSERT INTO team (id, name) VALUES (?, 'Email team')", team);
        String email = UUID.randomUUID() + "@example.test";
        insertUser(user, team, email);
        // Act / Assert
        assertThatThrownBy(() -> insertUser(UUID.randomUUID(), team, email.toUpperCase(java.util.Locale.ROOT)))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE team_id = ?", Integer.class, team)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT email FROM app_user WHERE id = ?", String.class, user)).isEqualTo(email);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"effort = 4", "title = E'\\t\\n '", "status = 'QA'", "priority = 'URGENT'"})
    void invalidTaskUpdatePreservesStoredRow(String update) {
        // Arrange
        var fixture = fixture();
        var before = jdbc.queryForMap("SELECT * FROM task WHERE id = ?", fixture[3]);
        // Act / Assert
        assertThatThrownBy(() -> jdbc.update("UPDATE task SET " + update + " WHERE id = ?", fixture[3]))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForMap("SELECT * FROM task WHERE id = ?", fixture[3])).isEqualTo(before);
    }

    @Test
    void secondAssignmentAndReferencedTeamDeletionAreRejected() {
        // Arrange
        var f = fixture();
        UUID other = UUID.randomUUID();
        insertUser(other, f[0], other + "@example.test");
        jdbc.update("INSERT INTO task_assignment (task_id, user_id) VALUES (?, ?)", f[3], f[1]);
        // Act / Assert
        assertThatThrownBy(() -> jdbc.update("INSERT INTO task_assignment (task_id, user_id) VALUES (?, ?)", f[3], other))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM team WHERE id = ?", f[0]))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT user_id FROM task_assignment WHERE task_id = ?", UUID.class, f[3])).isEqualTo(f[1]);
        assertThat(count("team", "id", f[0])).isEqualTo(1);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"project", "task"})
    void parentDeletionCascadesAndPreservesUsersAndTeams(String parent) {
        // Arrange
        var f = fixture();
        jdbc.update("INSERT INTO task_assignment (task_id, user_id) VALUES (?, ?)", f[3], f[1]);
        jdbc.update("INSERT INTO comment (id, task_id, author_id, content, created_at) VALUES (?, ?, ?, 'Comment', now())", UUID.randomUUID(), f[3], f[1]);
        // Act
        jdbc.update("DELETE FROM " + parent + " WHERE id = ?", parent.equals("project") ? f[2] : f[3]);
        // Assert
        assertThat(count("task", "id", f[3])).isZero();
        assertThat(count("comment", "task_id", f[3])).isZero();
        assertThat(count("task_assignment", "task_id", f[3])).isZero();
        assertThat(count("app_user", "id", f[1])).isEqualTo(1);
        assertThat(count("team", "id", f[0])).isEqualTo(1);
        assertThat(count("project", "id", f[2])).isEqualTo(parent.equals("project") ? 0 : 1);
    }

    private UUID[] fixture() {
        UUID team = UUID.randomUUID(), user = UUID.randomUUID(), project = UUID.randomUUID(), task = UUID.randomUUID();
        jdbc.update("INSERT INTO team (id, name) VALUES (?, 'Fixture team')", team);
        insertUser(user, team, user + "@example.test");
        jdbc.update("INSERT INTO project (id, name, description, team_id, owner_id) VALUES (?, 'Project', '', ?, ?)", project, team, user);
        insertTask(task, project);
        return new UUID[] {team, user, project, task};
    }

    @Test
    void repeatMigrationPreservesExistingRowsWithoutReseeding() {
        // Arrange
        String schema = "repeat_test";
        Flyway initial = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).defaultSchema(schema).load();
        initial.migrate();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO repeat_test.team (id, name) VALUES (?, 'Preserved')", id);

        // Act
        Flyway latest = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).defaultSchema(schema).load();
        var migrated = latest.migrate();

        // Assert
        assertThat(migrated.migrationsExecuted).isZero();
        assertThat(latest.info().pending()).isEmpty();
        latest.validate();
        assertThat(jdbc.queryForObject("SELECT name FROM repeat_test.team WHERE id = ?", String.class, id))
                .isEqualTo("Preserved");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM repeat_test.app_user WHERE provisioning_status = 'COMPLETED'", Integer.class)).isEqualTo(3);
    }

    @Test
    void checksumCorruptionIsDetected() {
        // Arrange
        String schema = "checksum_test";
        Flyway isolated = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).defaultSchema(schema).load();
        isolated.migrate();
        try {
            // Act
            jdbc.update("UPDATE checksum_test.flyway_schema_history SET checksum = checksum + 1 WHERE version = '1'");
            var validation = isolated.validateWithResult();
            // Assert
            assertThat(validation.validationSuccessful).isFalse();
            assertThat(validation.invalidMigrations).anySatisfy(migration ->
                    assertThat(migration.errorDetails.errorMessage).containsIgnoringCase("checksum"));
        } finally {
            jdbc.update("UPDATE checksum_test.flyway_schema_history SET checksum = checksum - 1 WHERE version = '1'");
            isolated.validate();
        }
    }

    private void insertUser(UUID id, UUID team, String email) {
        jdbc.update("INSERT INTO app_user (id, keycloak_subject, email, team_id, provisioning_status, provisioning_correlation_id) VALUES (?, ?, ?, ?, 'COMPLETED', ?)",
                id, id.toString(), email, team, UUID.randomUUID());
    }

    private void insertTask(UUID task, UUID project) {
        jdbc.update("INSERT INTO task (id, project_id, title, description, effort, created_at, updated_at) VALUES (?, ?, 'Task', '', 3, now(), now())", task, project);
    }

    private int count(String table, String column, UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, id);
    }
}
