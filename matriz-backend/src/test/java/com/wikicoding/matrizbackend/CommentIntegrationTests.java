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
class CommentIntegrationTests {
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

    @ParameterizedTest
    @ValueSource(ints = {1, 2000})
    void createsAndReopensCommentWithAuthenticatedAuthor(int length) throws Exception {
        // Arrange
        String content = "x".repeat(length);
        // Act / Assert: creation followed by independent HTTP reads and database verification.
        var response = mvc.perform(post("/api/v1/comments").header("Authorization", "Bearer SOFTWARE_ENGINEER")
                .contentType(MediaType.APPLICATION_JSON).content(body(taskId, content)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.authorId").value(author.toString()))
                .andExpect(jsonPath("$.content").value(content)).andReturn();
        UUID id = jdbc.queryForObject("SELECT id FROM comment WHERE task_id = ?", UUID.class, taskId);
        assertThat(mapper.readTree(response.getResponse().getContentAsString()).get("id").asText()).isEqualTo(id.toString());
        assertThat(jdbc.queryForMap("SELECT author_id, task_id, content FROM comment WHERE id = ?", id))
                .containsEntry("author_id", author).containsEntry("task_id", taskId).containsEntry("content", content);
        var timestamp = jdbc.queryForObject("SELECT created_at FROM comment WHERE id = ?", OffsetDateTime.class, id);
        String formatted = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault()).format(timestamp.toInstant());
        for (String role : List.of("SOFTWARE_ENGINEER", "PROJECT_MANAGER")) {
            mvc.perform(get("/api/v1/comments/{id}", id).header("Authorization", "Bearer " + role))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").value(content))
                    .andExpect(jsonPath("$.createdAt").value(formatted));
            mvc.perform(get("/api/v1/tasks/{id}/comments", taskId).header("Authorization", "Bearer " + role))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id.toString()))
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "oversized"})
    void rejectsInvalidContentWithoutPersisting(String value) throws Exception {
        // Arrange
        String content = value.equals("oversized") ? "x".repeat(2001) : value;
        // Act / Assert
        mvc.perform(post("/api/v1/comments").header("Authorization", "Bearer SOFTWARE_ENGINEER")
                .contentType(MediaType.APPLICATION_JSON).content(body(taskId, content)))
                .andExpect(status().isBadRequest());
        assertThat(count(taskId)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROJECT_MANAGER", "ADMINISTRATOR", "anonymous"})
    void rejectsUnauthorizedCreationWithoutPersisting(String role) throws Exception {
        // Arrange
        var request = post("/api/v1/comments").contentType(MediaType.APPLICATION_JSON).content(body(taskId, "Denied"));
        if (!role.equals("anonymous")) request.header("Authorization", "Bearer " + role);
        // Act / Assert
        mvc.perform(request).andExpect(status().is(role.equals("anonymous") ? 401 : 403));
        assertThat(count(taskId)).isZero();
    }

    @Test
    void missingResourcesReturnNotFound() throws Exception {
        // Arrange
        UUID missing = UUID.randomUUID();
        // Act / Assert
        mvc.perform(post("/api/v1/comments").header("Authorization", "Bearer SOFTWARE_ENGINEER")
                .contentType(MediaType.APPLICATION_JSON).content(body(missing, "Missing task")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tasks/{id}/comments", missing).header("Authorization", "Bearer SOFTWARE_ENGINEER"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/comments/{id}", missing).header("Authorization", "Bearer PROJECT_MANAGER"))
                .andExpect(status().isNotFound());
        assertThat(count(missing)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"tasks", "projects"})
    void parentDeletionCascadesOnlyItsComments(String parent) throws Exception {
        // Arrange
        UUID otherProject = project(teamId, author, "Retained");
        UUID otherTask = task(otherProject, "Retained", Status.TODO, Priority.LOW, Instant.parse("2026-01-01T00:00:00Z"));
        UUID removed = seedComment(taskId);
        UUID siblingTask = task(projectId, "Sibling", Status.TODO, Priority.LOW, Instant.parse("2026-01-01T00:00:00Z"));
        UUID siblingComment = seedComment(siblingTask);
        UUID retained = seedComment(otherTask);
        UUID deleted = parent.equals("tasks") ? taskId : projectId;
        // Act / Assert
        mvc.perform(delete("/api/v1/" + parent + "/" + deleted).header("Authorization", "Bearer PROJECT_MANAGER"))
                .andExpect(status().isNoContent());
        assertThat(count(taskId)).isZero();
        assertThat(count(otherTask)).isEqualTo(1);
        assertThat(count(siblingTask)).isEqualTo(parent.equals("tasks") ? 1 : 0);
        mvc.perform(get("/api/v1/comments/{id}", removed).header("Authorization", "Bearer SOFTWARE_ENGINEER"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/comments/{id}", siblingComment).header("Authorization", "Bearer SOFTWARE_ENGINEER"))
                .andExpect(status().is(parent.equals("tasks") ? 200 : 404));
        mvc.perform(get("/api/v1/comments/{id}", retained).header("Authorization", "Bearer SOFTWARE_ENGINEER"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id = ?", Integer.class, author)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team WHERE id = ?", Integer.class, teamId)).isEqualTo(1);
    }

    @Test
    void listingIsIsolatedAndProtected() throws Exception {
        // Arrange
        UUID selected = seedComment(taskId);
        UUID emptyTask = task(projectId, "Empty", Status.TODO, Priority.LOW, Instant.parse("2026-01-01T00:00:00Z"));
        // Act / Assert
        mvc.perform(get("/api/v1/tasks/{id}/comments", emptyTask).header("Authorization", "Bearer SOFTWARE_ENGINEER"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        for (String path : List.of("/api/v1/comments/" + selected, "/api/v1/tasks/" + taskId + "/comments")) {
            mvc.perform(get(path).header("Authorization", "Bearer ADMINISTRATOR")).andExpect(status().isForbidden());
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        assertThat(count(taskId)).isEqualTo(1);
    }

    @Test
    void listsCommentsChronologicallyWithStableTieBreakAndTaskIsolation() throws Exception {
        // Arrange: deliberately insert in a different order from the expected result.
        UUID late = seedComment(taskId);
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jdbc.update("UPDATE comment SET created_at = TIMESTAMPTZ '2026-01-02 00:00:00+00' WHERE id = ?", late);
        for (UUID id : List.of(second, first)) {
            jdbc.update("INSERT INTO comment (id, task_id, author_id, content, created_at) VALUES (?, ?, ?, 'Ordered', TIMESTAMPTZ '2026-01-01 00:00:00+00')",
                    id, taskId, author);
        }
        UUID otherTask = task(projectId, "Other", Status.TODO, Priority.LOW, Instant.parse("2026-01-01T00:00:00Z"));
        seedComment(otherTask);
        // Act / Assert
        mvc.perform(get("/api/v1/tasks/{id}/comments", taskId).header("Authorization", "Bearer SOFTWARE_ENGINEER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(first.toString()))
                .andExpect(jsonPath("$[1].id").value(second.toString()))
                .andExpect(jsonPath("$[2].id").value(late.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missingContent", "nullContent", "missingTask", "nullTask", "invalidTask"})
    void rejectsInvalidRequestFieldsWithoutWriting(String scenario) throws Exception {
        // Arrange
        var payload = new java.util.HashMap<String, Object>();
        payload.put("taskId", taskId);
        payload.put("content", "Valid");
        switch (scenario) {
            case "missingContent" -> payload.remove("content");
            case "nullContent" -> payload.put("content", null);
            case "missingTask" -> payload.remove("taskId");
            case "nullTask" -> payload.put("taskId", null);
            case "invalidTask" -> payload.put("taskId", "not-a-uuid");
            default -> throw new IllegalArgumentException(scenario);
        }
        // Act / Assert
        mvc.perform(post("/api/v1/comments").header("Authorization", "Bearer SOFTWARE_ENGINEER")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        assertThat(count(taskId)).isZero();
    }

    @Test
    void rejectsEngineerWithoutLocalProfile() throws Exception {
        // Arrange
        when(decoder.decode("unknown")).thenReturn(Jwt.withTokenValue("unknown").header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .claim("realm_access", Map.of("roles", List.of("SOFTWARE_ENGINEER"))).build());
        // Act / Assert
        mvc.perform(post("/api/v1/comments").header("Authorization", "Bearer unknown")
                .contentType(MediaType.APPLICATION_JSON).content(body(taskId, "Rejected")))
                .andExpect(status().isForbidden());
        assertThat(count(taskId)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void individualMutationIsForbiddenAndPreservesComment(String method) throws Exception {
        // Arrange
        UUID id = seedComment(taskId);
        var before = jdbc.queryForMap("SELECT * FROM comment WHERE id = ?", id);
        // Act / Assert
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), "/api/v1/comments/" + id)
                .header("Authorization", "Bearer SOFTWARE_ENGINEER")
                .contentType(MediaType.APPLICATION_JSON).content(body(taskId, "Changed")))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForMap("SELECT * FROM comment WHERE id = ?", id)).isEqualTo(before);
    }

    @Test
    void preservesMultilineContentAndIgnoresSuppliedTimestamp() throws Exception {
        // Arrange
        String content = "Line one\n\"Quoted\" and \\ backslash";
        String supplied = "01/01/2000 00:00:00";
        String payload = mapper.writeValueAsString(Map.of("taskId", taskId, "content", content, "createdAt", supplied));
        // Act / Assert
        mvc.perform(post("/api/v1/comments").header("Authorization", "Bearer SOFTWARE_ENGINEER")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.content").value(content));
        assertThat(jdbc.queryForObject("SELECT content FROM comment WHERE task_id = ?", String.class, taskId)).isEqualTo(content);
        assertThat(jdbc.queryForObject("SELECT created_at FROM comment WHERE task_id = ?", OffsetDateTime.class, taskId)
                .toInstant()).isAfter(Instant.parse("2000-01-01T00:00:00Z"));
    }

    private String body(UUID task, String content) throws Exception {
        return mapper.writeValueAsString(Map.of("taskId", task, "content", content,
                "authorId", UUID.randomUUID()));
    }

    private int count(UUID task) {
        return jdbc.queryForObject("SELECT count(*) FROM comment WHERE task_id = ?", Integer.class, task);
    }

    private UUID seedComment(UUID task) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO comment (id, task_id, author_id, content, created_at) VALUES (?, ?, ?, 'Persisted', TIMESTAMPTZ '2026-01-01 00:00:00+00')", id, task, author);
        return id;
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
