package com.wikicoding.matrizbackend;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import com.wikicoding.matrizbackend.projectmanagement.dtos.tasks.TaskUpdateRequest;
import com.wikicoding.matrizbackend.projectmanagement.UserEligibility;
import com.wikicoding.matrizbackend.projectmanagement.services.AssignmentService;
import com.wikicoding.matrizbackend.projectmanagement.services.ProjectService;
import com.wikicoding.matrizbackend.projectmanagement.services.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class ProjectManagementIntegrationTests {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ProjectService projects;
    @Autowired TaskService tasks;
    @Autowired AssignmentService assignments;
    @MockitoBean UserEligibility eligibility;

    @Test
    void changingProjectTeamRemovesInvalidAssignmentsWithoutTouchingOtherProjects() {
        // Arrange
        UUID oldTeam = team("Old team"), newTeam = team("New team");
        UUID owner = user(oldTeam), oldTeamEngineer = user(oldTeam), newTeamEngineer = user(newTeam);
        UUID project = project(oldTeam, owner, "Team change");
        UUID unrelatedProject = project(newTeam, owner, "Unrelated project");
        UUID removedTask = task(project, "Removed assignment", Status.TODO, Priority.MEDIUM,
                Instant.parse("2026-01-01T00:00:00Z"));
        UUID retainedTask = task(unrelatedProject, "Retained assignment", Status.TODO, Priority.MEDIUM,
                Instant.parse("2026-01-02T00:00:00Z"));
        assign(removedTask, oldTeamEngineer);
        assign(retainedTask, newTeamEngineer);

        // Act
        projects.updateProject(project, "Team change", "", newTeam);

        // Assert
        assertThat(projects.findProjectById(project).getTeamId()).isEqualTo(newTeam);
        assertThat(assignee(removedTask)).isNull();
        assertThat(assignee(retainedTask)).isEqualTo(newTeamEngineer);
        assertThat(tasks.findTaskById(removedTask).getProjectId()).isEqualTo(project);
    }

    @Test
    void movingTaskRemovesIneligibleAssignment() {
        // Arrange
        UUID firstTeam = team("Source team"), secondTeam = team("Destination team");
        UUID owner = user(firstTeam), engineer = user(firstTeam);
        UUID source = project(firstTeam, owner, "Source project");
        UUID destination = project(secondTeam, owner, "Destination project");
        UUID task = task(source, "Moving task", Status.TODO, Priority.LOW,
                Instant.parse("2026-01-01T00:00:00Z"));
        assign(task, engineer);

        // Act
        tasks.updateTask(task, new TaskUpdateRequest(null, null, null, null, null, destination));

        // Assert
        assertThat(tasks.findTaskById(task).getProjectId()).isEqualTo(destination);
        assertThat(assignee(task)).isNull();
    }

    @Test
    void rejectedTaskMovePreservesProjectAndAssignment() {
        // Arrange
        UUID firstTeam = team("Rejected source"), secondTeam = team("Rejected destination");
        UUID owner = user(firstTeam), engineer = user(firstTeam);
        UUID source = project(firstTeam, owner, "Original project");
        UUID destination = project(secondTeam, owner, "Target project");
        UUID task = task(source, "Unchanged task", Status.TODO, Priority.LOW,
                Instant.parse("2026-01-01T00:00:00Z"));
        assign(task, engineer);

        // Act / Assert
        assertThatThrownBy(() -> tasks.updateTask(task,
                new TaskUpdateRequest("Unchanged task", "", (short) 4, null, null, destination)))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode().value()).isEqualTo(400));
        assertThat(tasks.findTaskById(task).getProjectId()).isEqualTo(source);
        assertThat(assignee(task)).isEqualTo(engineer);
    }

    @Test
    void taskFiltersAndPaginationReadPersistedRowsFromOnlySelectedProject() {
        // Arrange
        UUID team = team("Listing team"), owner = user(team);
        UUID selected = project(team, owner, "Selected project");
        UUID other = project(team, owner, "Other project");
        UUID older = task(selected, "Older high", Status.TODO, Priority.HIGH,
                Instant.parse("2026-01-01T00:00:00Z"));
        UUID newer = task(selected, "Newer high", Status.TODO, Priority.HIGH,
                Instant.parse("2026-01-02T00:00:00Z"));
        task(selected, "Done high", Status.DONE, Priority.HIGH,
                Instant.parse("2026-01-03T00:00:00Z"));
        task(selected, "Low todo", Status.TODO, Priority.LOW,
                Instant.parse("2026-01-04T00:00:00Z"));
        task(other, "Other high", Status.TODO, Priority.HIGH,
                Instant.parse("2026-01-05T00:00:00Z"));

        // Act
        var first = tasks.findAllTasks(selected, Status.TODO, Priority.HIGH, 0, 1);
        var second = tasks.findAllTasks(selected, Status.TODO, Priority.HIGH, 1, 1);
        var beyond = tasks.findAllTasks(selected, Status.TODO, Priority.HIGH, 2, 1);

        // Assert
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getContent()).extracting("id").containsExactly(newer);
        assertThat(second.getContent()).extracting("id").containsExactly(older);
        assertThat(beyond.getContent()).isEmpty();
    }

    @Test
    void secondAssigneeConflictsAndRemovalPreservesTheUser() {
        // Arrange
        UUID team = team("Assignment team"), owner = user(team);
        UUID firstEngineer = user(team), secondEngineer = user(team);
        UUID project = project(team, owner, "Assignment project");
        UUID task = task(project, "Assigned task", Status.TODO, Priority.MEDIUM,
                Instant.parse("2026-01-01T00:00:00Z"));

        // Act
        assignments.add(task, firstEngineer);
        ResponseStatusException conflict = catchThrowableOfType(
                () -> assignments.add(task, secondEngineer), ResponseStatusException.class);
        UUID assigneeAfterConflict = assignee(task);
        ResponseStatusException wrongRemoval = catchThrowableOfType(
                () -> assignments.remove(task, secondEngineer), ResponseStatusException.class);
        UUID assigneeAfterWrongRemoval = assignee(task);
        assignments.remove(task, firstEngineer);

        // Assert
        verify(eligibility).requireSoftwareEngineerInTeam(firstEngineer, team);
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        assertThat(assigneeAfterConflict).isEqualTo(firstEngineer);
        assertThat(wrongRemoval.getStatusCode().value()).isEqualTo(404);
        assertThat(assigneeAfterWrongRemoval).isEqualTo(firstEngineer);
        assertThat(assignee(task)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id = ?", Integer.class, firstEngineer))
                .isEqualTo(1);
    }

    @Test
    void concurrentAssignmentsHaveExactlyOneWinner() throws Exception {
        // Arrange: the barrier puts both transactions past the initial existence check.
        UUID team = team("Concurrent"), owner = user(team);
        UUID task = task(project(team, owner, "Concurrent"), "Concurrent", Status.TODO, Priority.LOW, Instant.EPOCH);
        UUID first = user(team), second = user(team);
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        org.mockito.Mockito.doAnswer(call -> { barrier.await(10, java.util.concurrent.TimeUnit.SECONDS); return null; })
                .when(eligibility).requireSoftwareEngineerInTeam(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(team));
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            // Act
            var outcomes = executor.invokeAll(java.util.List.of(
                    (java.util.concurrent.Callable<Integer>) () -> assignOutcome(task, first),
                    () -> assignOutcome(task, second)), 20, java.util.concurrent.TimeUnit.SECONDS);
            // Assert
            assertThat(java.util.List.of(outcomes.get(0).get(), outcomes.get(1).get()))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(assignee(task)).isIn(first, second);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_assignment WHERE task_id = ?", Integer.class, task)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    private int assignOutcome(UUID task, UUID user) {
        try { assignments.add(task, user); return 201; }
        catch (ResponseStatusException error) { return error.getStatusCode().value(); }
    }

    @Test
    void databaseFailureAfterCleanupRollsBackTaskMoveAndAssignment() {
        // Arrange: a disposable constraint rejects the final write after assignment cleanup.
        UUID team = team("Rollback"), otherTeam = team("Rollback target"), engineer = user(team);
        UUID source = project(team, engineer, "Source"), destination = project(otherTeam, engineer, "Target");
        UUID task = task(source, "Rollback", Status.TODO, Priority.LOW, Instant.EPOCH);
        assign(task, engineer);
        String constraint = "test_move_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("ALTER TABLE task ADD CONSTRAINT " + constraint + " CHECK (id <> '" + task + "'::uuid OR project_id <> '" + destination + "'::uuid)");
        try {
            // Act / Assert
            assertThatThrownBy(() -> tasks.updateTask(task, new TaskUpdateRequest(null, null, null, null, null, destination)))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(tasks.findTaskById(task).getProjectId()).isEqualTo(source);
            assertThat(assignee(task)).isEqualTo(engineer);
        } finally {
            jdbc.execute("ALTER TABLE task DROP CONSTRAINT " + constraint);
        }
    }

    @Test
    void movingWithinSameTeamPreservesAssignment() {
        // Arrange
        UUID team = team("Same team"), engineer = user(team);
        UUID source = project(team, engineer, "Source"), destination = project(team, engineer, "Target");
        UUID task = task(source, "Move", Status.TODO, Priority.LOW, Instant.EPOCH);
        assign(task, engineer);
        // Act
        tasks.updateTask(task, new TaskUpdateRequest(null, null, null, null, null, destination));
        // Assert
        assertThat(tasks.findTaskById(task).getProjectId()).isEqualTo(destination);
        assertThat(assignee(task)).isEqualTo(engineer);
    }

    @Test
    void independentFiltersAndStablePaginationReadPersistedTasks() {
        // Arrange
        UUID team = team("Filters"), owner = user(team), project = project(team, owner, "Filters");
        UUID one = task(project, "One", Status.TODO, Priority.HIGH, Instant.EPOCH);
        UUID two = task(project, "Two", Status.DONE, Priority.HIGH, Instant.EPOCH);
        UUID three = task(project, "Three", Status.TODO, Priority.LOW, Instant.EPOCH);
        // Act
        var status = tasks.findAllTasks(project, Status.TODO, null, 0, 100);
        var priority = tasks.findAllTasks(project, null, Priority.HIGH, 0, 100);
        var first = tasks.findAllTasks(project, null, null, 0, 2);
        var second = tasks.findAllTasks(project, null, null, 1, 2);
        // Assert: PostgreSQL compares UUIDs by their unsigned byte representation.
        assertThat(status.getContent()).extracting("id").containsExactlyInAnyOrder(one, three);
        assertThat(priority.getContent()).extracting("id").containsExactlyInAnyOrder(one, two);
        var expected = java.util.stream.Stream.of(one, two, three).sorted(java.util.Comparator.comparing(UUID::toString)).toList();
        assertThat(first.getContent()).extracting("id").containsExactlyElementsOf(expected.subList(0, 2));
        assertThat(second.getContent()).extracting("id").containsExactly(expected.get(2));
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.isLast()).isTrue();
    }

    @Autowired com.wikicoding.matrizbackend.user.services.UserService users;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"engineer", "wrongTeam", "wrongRole", "unavailable"})
    void eligibilityAdapterUsesPersistedTeamAndProviderRoles(String scenario) {
        // Arrange: only the identity provider response is simulated; user lookup uses PostgreSQL.
        UUID team = team("Eligibility"), owner = user(team);
        UUID candidate = user(scenario.equals("wrongTeam") ? team("Other") : team);
        UUID task = task(project(team, owner, "Eligibility"), "Eligibility", Status.TODO, Priority.LOW, Instant.EPOCH);
        var client = org.springframework.web.reactive.function.client.WebClient.builder().exchangeFunction(request -> {
            if (scenario.equals("unavailable")) return reactor.core.publisher.Mono.error(new IllegalStateException("Provider unavailable"));
            String body = request.url().getPath().endsWith("/token") ? "{\"access_token\":\"test\"}"
                    : scenario.equals("wrongRole") ? "[{\"name\":\"PROJECT_MANAGER\"}]" : "[{\"name\":\"SOFTWARE_ENGINEER\"}]";
            return reactor.core.publisher.Mono.just(org.springframework.web.reactive.function.client.ClientResponse
                    .create(org.springframework.http.HttpStatus.OK).header("Content-Type", "application/json").body(body).build());
        }).build();
        var adapter = new com.wikicoding.matrizbackend.user.services.KeycloakUserEligibility(users, client, "test", "test", "test");
        org.mockito.Mockito.doAnswer(call -> { adapter.requireSoftwareEngineerInTeam(call.getArgument(0), call.getArgument(1)); return null; })
                .when(eligibility).requireSoftwareEngineerInTeam(candidate, team);
        // Act
        int outcome = assignOutcome(task, candidate);
        // Assert
        assertThat(outcome).isEqualTo(scenario.equals("engineer") ? 201 : scenario.equals("unavailable") ? 503 : 400);
        assertThat(assignee(task)).isEqualTo(scenario.equals("engineer") ? candidate : null);
    }

    private UUID team(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO team (id, name) VALUES (?, ?)", id, name);
        return id;
    }

    private UUID user(UUID teamId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, keycloak_subject, email, team_id, provisioning_status, provisioning_correlation_id) " +
                "VALUES (?, ?, ?, ?, 'COMPLETED', ?)", id, id.toString(), id + "@example.test", teamId, UUID.randomUUID());
        return id;
    }

    private UUID project(UUID teamId, UUID ownerId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO project (id, name, description, team_id, owner_id) VALUES (?, ?, '', ?, ?)",
                id, name, teamId, ownerId);
        return id;
    }

    private UUID task(UUID projectId, String title, Status status, Priority priority, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO task (id, project_id, title, description, effort, status, priority, created_at, updated_at) " +
                "VALUES (?, ?, ?, '', 3, ?, ?, ?, ?)", id, projectId, title, status.name(), priority.name(),
                OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC), OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
        return id;
    }

    private void assign(UUID taskId, UUID userId) {
        jdbc.update("INSERT INTO task_assignment (task_id, user_id) VALUES (?, ?)", taskId, userId);
    }

    private UUID assignee(UUID taskId) {
        return jdbc.query("SELECT user_id FROM task_assignment WHERE task_id = ?",
                result -> result.next() ? result.getObject(1, UUID.class) : null, taskId);
    }
}
