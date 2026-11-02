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
                .isInstanceOf(ResponseStatusException.class);
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
