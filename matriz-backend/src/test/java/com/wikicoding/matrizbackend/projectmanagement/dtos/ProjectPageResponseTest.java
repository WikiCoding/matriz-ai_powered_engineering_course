package com.wikicoding.matrizbackend.projectmanagement.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicoding.matrizbackend.projectmanagement.domain.project.Project;
import java.util.List;
import java.util.UUID;

import com.wikicoding.matrizbackend.projectmanagement.dtos.projects.ProjectPageResponse;
import com.wikicoding.matrizbackend.projectmanagement.dtos.projects.ProjectResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ProjectPageResponseTest {
    @Test
    void mapsProjectPageToExplicitResponseFields() {
        // Arrange
        var project = new Project(UUID.randomUUID(), "Alpha", "Description", UUID.randomUUID(), UUID.randomUUID());
        var page = new PageImpl<>(List.of(project), PageRequest.of(1, 20), 21);
        // Act
        var response = ProjectPageResponse.from(page);

        // Assert
        assertThat(response.content()).extracting(ProjectResponse::id).containsExactly(project.getId());
        assertThat(response.totalElements()).isEqualTo(21);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.number()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
    }
}
