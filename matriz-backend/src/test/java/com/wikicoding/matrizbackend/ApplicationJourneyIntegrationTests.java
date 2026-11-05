package com.wikicoding.matrizbackend;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Priority;
import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@org.springframework.transaction.annotation.Transactional
class ApplicationJourneyIntegrationTests {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @MockitoBean JwtDecoder decoder;
    @Autowired tools.jackson.databind.ObjectMapper mapper;
    MockMvc mvc;
    String subject;
    UUID teamId, author, projectId, taskId;

    @BeforeEach
    void arrangeFixtures() {
        // Arrange: real persisted fixtures and simulated identity-provider tokens.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
        teamId = team("Comments " + UUID.randomUUID());
        author = user(team("Author " + UUID.randomUUID()));
        subject = jdbc.queryForObject("SELECT keycloak_subject FROM app_user WHERE id = ?", String.class, author);
        projectId = project(teamId, user(teamId), "Comments project");
        taskId = task(projectId, "Comments task", Status.TODO, Priority.LOW, Instant.parse("2026-01-01T00:00:00Z"));
        for (String role : List.of("SOFTWARE_ENGINEER", "PROJECT_MANAGER", "ADMINISTRATOR")) {
            when(decoder.decode(role)).thenReturn(Jwt.withTokenValue(role).header("alg", "RS256")
                    .subject(subject).claim("realm_access", Map.of("roles", List.of(role)))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build());
        }
    }

    @Autowired com.wikicoding.matrizbackend.user.services.UserService users;
    @Autowired com.wikicoding.matrizbackend.team.services.TeamService teams;
    @Autowired com.wikicoding.matrizbackend.projectmanagement.services.DashboardService dashboard;

    @Test
    void usersAndTeamsPersistAndListingsMatchDatabaseOrder() {
        // Arrange
        var team = teams.createTeam("Created " + UUID.randomUUID());
        var profile = users.createUser(UUID.randomUUID().toString(), UUID.randomUUID() + "@example.test", team.getId());
        var expectedTeams = jdbc.queryForList("SELECT id FROM team ORDER BY name, id", UUID.class);
        var expectedUsers = jdbc.queryForList("SELECT id FROM app_user WHERE deleted = false AND provisioning_status = 'COMPLETED' ORDER BY email, id", UUID.class);
        // Act
        var firstTeams = teams.findAllTeamsPagedAndSorted(0, 2);
        var nextTeams = teams.findAllTeamsPagedAndSorted(1, 2);
        var firstUsers = users.findAllUsersPagedAndSorted(0, 2);
        var nextUsers = users.findAllUsersPagedAndSorted(1, 2);
        // Assert
        assertThat(teams.findTeamById(team.getId()).getName()).isEqualTo(team.getName());
        assertThat(users.requireActive(profile.id()).teamId()).isEqualTo(team.getId());
        assertThat(firstTeams.getContent()).extracting("id").containsExactlyElementsOf(expectedTeams.subList(0, 2));
        assertThat(nextTeams.getContent()).extracting("id").containsExactlyElementsOf(expectedTeams.subList(2, Math.min(4, expectedTeams.size())));
        assertThat(firstUsers.getContent()).extracting("id").containsExactlyElementsOf(expectedUsers.subList(0, 2));
        assertThat(nextUsers.getContent()).extracting("id").containsExactlyElementsOf(expectedUsers.subList(2, 4));
        assertThat(firstTeams.getTotalElements()).isEqualTo(expectedTeams.size());
        assertThat(firstUsers.getTotalElements()).isEqualTo(expectedUsers.size());
    }

    @Test
    void dashboardCountsCurrentDatabaseStateAndHidesContentFromAdministrator() {
        // Arrange
        var before = dashboard.getDashboard(false);
        task(projectId, "Done", Status.DONE, Priority.HIGH, Instant.EPOCH);
        task(projectId, "Started", Status.IN_PROGRESS, Priority.LOW, Instant.EPOCH);
        // Act
        var actual = dashboard.getDashboard(false);
        var admin = dashboard.getDashboard(true);
        // Assert
        assertThat(actual.taskCounts().get("DONE")).isEqualTo(before.taskCounts().get("DONE") + 1);
        assertThat(actual.taskCounts().get("IN_PROGRESS")).isEqualTo(before.taskCounts().get("IN_PROGRESS") + 1);
        assertThat(actual.taskCounts().get("TODO")).isEqualTo(before.taskCounts().get("TODO"));
        assertThat(actual.projects()).extracting("id").containsExactlyElementsOf(
                jdbc.queryForList("SELECT id FROM project ORDER BY name, id", UUID.class));
        assertThat(admin.projects()).isEmpty();
        assertThat(admin.taskCounts()).containsOnly(Map.entry("TODO", 0L), Map.entry("IN_PROGRESS", 0L), Map.entry("DONE", 0L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOFTWARE_ENGINEER", "ADMINISTRATOR", "anonymous"})
    void projectAndTaskWritesAreRestrictedToManagers(String role) throws Exception {
        // Arrange
        var beforeProject = jdbc.queryForMap("SELECT * FROM project WHERE id = ?", projectId);
        var beforeTask = jdbc.queryForMap("SELECT * FROM task WHERE id = ?", taskId);
        var requests = List.of(
                post("/api/v1/projects").content(mapper.writeValueAsString(Map.of("name", "Denied", "description", "", "teamId", teamId))),
                post("/api/v1/tasks").content(mapper.writeValueAsString(Map.of("title", "Denied", "description", "", "effort", 3, "projectId", projectId))),
                put("/api/v1/projects/" + projectId).content(mapper.writeValueAsString(Map.of("name", "Denied", "description", "", "teamId", teamId))),
                patch("/api/v1/tasks/" + taskId).content("{\"title\":\"Denied\"}"),
                delete("/api/v1/projects/" + projectId), delete("/api/v1/tasks/" + taskId));
        int projectsBefore = jdbc.queryForObject("SELECT count(*) FROM project", Integer.class);
        int tasksBefore = jdbc.queryForObject("SELECT count(*) FROM task", Integer.class);
        // Act / Assert
        for (var request : requests) {
            if (!role.equals("anonymous")) request.header("Authorization", "Bearer " + role);
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(role.equals("anonymous") ? 401 : 403));
        }
        assertThat(jdbc.queryForMap("SELECT * FROM project WHERE id = ?", projectId)).isEqualTo(beforeProject);
        assertThat(jdbc.queryForMap("SELECT * FROM task WHERE id = ?", taskId)).isEqualTo(beforeTask);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project", Integer.class)).isEqualTo(projectsBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM task", Integer.class)).isEqualTo(tasksBefore);
    }

    @Test
    void managerCreatesProjectAndTaskAndValidationPreservesTask() throws Exception {
        // Arrange
        String projectBody = mapper.writeValueAsString(Map.of("name", "Created project", "description", "", "teamId", teamId));
        // Act / Assert: create and reopen a complete project/task workflow.
        var created = mvc.perform(post("/api/v1/projects").header("Authorization", "Bearer PROJECT_MANAGER")
                .contentType(MediaType.APPLICATION_JSON).content(projectBody)).andExpect(status().isCreated()).andReturn();
        String project = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        var taskResponse = mvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer PROJECT_MANAGER")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("projectId", project,
                        "title", "Created task", "description", "", "effort", 3))))
                .andExpect(status().isCreated()).andReturn();
        String task = mapper.readTree(taskResponse.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(patch("/api/v1/tasks/" + task).header("Authorization", "Bearer PROJECT_MANAGER")
                .contentType(MediaType.APPLICATION_JSON).content("{\"effort\":4}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/tasks/" + task).header("Authorization", "Bearer SOFTWARE_ENGINEER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.effort").value(3))
                .andExpect(jsonPath("$.projectId").value(project));
        mvc.perform(get("/api/v1/projects/" + project).header("Authorization", "Bearer SOFTWARE_ENGINEER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(author.toString()));
        mvc.perform(get("/api/v1/projects/" + project).header("Authorization", "Bearer ADMINISTRATOR"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tasks/" + task).header("Authorization", "Bearer ADMINISTRATOR"))
                .andExpect(status().isForbidden());
    }

    private UUID team(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO team (id, name) VALUES (?, ?)", id, name);
        return id;
    }

    private UUID user(UUID teamId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, keycloak_subject, email, team_id, provisioning_status, provisioning_correlation_id) " +
                "VALUES (?, ?, ?, ?, 'COMPLETED', ?)", id, UUID.randomUUID().toString(), id + "@example.test", teamId, UUID.randomUUID());
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

}
