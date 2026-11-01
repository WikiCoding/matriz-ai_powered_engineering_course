package com.wikicoding.matrizbackend.projectmanagement.dtos.projects;

import com.wikicoding.matrizbackend.projectmanagement.domain.Project;
import java.util.List;
import org.springframework.data.domain.Page;

public record ProjectPageResponse(List<ProjectResponse> content, long totalElements, int totalPages,
        int number, int size) {
    public static ProjectPageResponse from(Page<Project> page) {
        return new ProjectPageResponse(page.getContent().stream().map(ProjectResponse::from).toList(),
                page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
