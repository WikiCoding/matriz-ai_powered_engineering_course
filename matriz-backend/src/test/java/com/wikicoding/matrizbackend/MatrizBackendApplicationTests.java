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
        assertThat(flyway.info().applied()).hasSize(1);
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT to_regclass('public.event_publication') IS NULL", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT to_regclass('public.audit_record') IS NULL", Boolean.class)).isTrue();
    }

    @Test
    void constraintsAndCascadesPreserveUsersAndTeams() {
        UUID team = UUID.randomUUID(), user = UUID.randomUUID(), other = UUID.randomUUID();
        UUID project = UUID.randomUUID(), task = UUID.randomUUID();
        jdbc.update("INSERT INTO team VALUES (?, 'Test team')", team);
        insertUser(user, team, "one@example.test");
        insertUser(other, team, "two@example.test");
        assertThatThrownBy(() -> insertUser(UUID.randomUUID(), team, "ONE@example.test"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        jdbc.update("INSERT INTO project VALUES (?, 'Project', '', ?, ?)", project, team, user);
        insertTask(task, project);
        assertThat(jdbc.queryForObject("SELECT status || ':' || priority FROM task WHERE id = ?", String.class, task))
                .isEqualTo("TODO:MEDIUM");
        jdbc.update("INSERT INTO task_assignment VALUES (?, ?)", task, user);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO task_assignment VALUES (?, ?)", task, other))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM team WHERE id = ?", team))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        for (String invalid : new String[] {"UPDATE task SET effort = 4 WHERE id = ?",
                "UPDATE task SET title = E'\\t\\n ' WHERE id = ?",
                "UPDATE task SET status = 'QA' WHERE id = ?",
                "UPDATE task SET priority = 'URGENT' WHERE id = ?"}) {
            assertThatThrownBy(() -> jdbc.update(invalid, task))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
        jdbc.update("INSERT INTO comment VALUES (?, ?, ?, 'Comment', now())", UUID.randomUUID(), task, user);
        jdbc.update("DELETE FROM project WHERE id = ?", project);
        assertThat(count("task", "id", task)).isZero();
        assertThat(count("comment", "task_id", task)).isZero();
        assertThat(count("task_assignment", "task_id", task)).isZero();
        assertThat(count("app_user", "id", user)).isEqualTo(1);
        assertThat(count("team", "id", team)).isEqualTo(1);

        // Task deletion must also cascade without deleting its project.
        jdbc.update("INSERT INTO project VALUES (?, 'Project', '', ?, ?)", project, team, user);
        insertTask(task, project);
        jdbc.update("INSERT INTO task_assignment VALUES (?, ?)", task, user);
        jdbc.update("INSERT INTO comment VALUES (?, ?, ?, 'Comment', now())", UUID.randomUUID(), task, user);
        jdbc.update("DELETE FROM task WHERE id = ?", task);
        assertThat(count("comment", "task_id", task)).isZero();
        assertThat(count("task_assignment", "task_id", task)).isZero();
        assertThat(count("project", "id", project)).isEqualTo(1);
    }

    @Test
    void migrationRetainsExistingRowsAndDetectsChecksumChanges() {
        String schema = "upgrade_test";
        Flyway initial = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).defaultSchema(schema).target("1").load();
        initial.migrate();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO upgrade_test.team VALUES (?, 'Preserved')", id);
        Flyway latest = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).defaultSchema(schema).load();
        assertThat(latest.migrate().migrationsExecuted).isZero();
        latest.validate();
        assertThat(jdbc.queryForObject("SELECT name FROM upgrade_test.team WHERE id = ?", String.class, id))
                .isEqualTo("Preserved");
        jdbc.update("UPDATE upgrade_test.flyway_schema_history SET checksum = checksum + 1 WHERE version = '1'");
        assertThat(latest.validateWithResult().validationSuccessful).isFalse();
        jdbc.update("UPDATE upgrade_test.flyway_schema_history SET checksum = checksum - 1 WHERE version = '1'");
        latest.validate();
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
