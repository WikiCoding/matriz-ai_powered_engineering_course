package com.wikicoding.matrizbackend.projectmanagement.services;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Status;
import com.wikicoding.matrizbackend.projectmanagement.dtos.projects.DashboardResponse;
import com.wikicoding.matrizbackend.projectmanagement.dtos.projects.ProjectResponse;
import com.wikicoding.matrizbackend.projectmanagement.domain.project.Project;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectDataModel;
import com.wikicoding.matrizbackend.projectmanagement.persistence.task.TaskRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(boolean administrator) {
        if (administrator) {
            return new DashboardResponse(List.of(), Map.of("TODO", 0L, "IN_PROGRESS", 0L, "DONE", 0L));
        }

        List<ProjectResponse> accessible = projectRepository.findAll(Sort.by(Sort.Order.asc("name"),
                        Sort.Order.asc("id")))
                .stream().map(this::toResponse).toList();

        Map<Status, Long> totals = new EnumMap<>(Status.class);

        for (TaskRepository.StatusCount count : taskRepository.countByStatus()) {
            totals.put(count.getStatus(), count.getTotal());
        }

        return new DashboardResponse(accessible, Map.of(
                "TODO", totals.getOrDefault(Status.TODO, 0L),
                "IN_PROGRESS", totals.getOrDefault(Status.IN_PROGRESS, 0L),
                "DONE", totals.getOrDefault(Status.DONE, 0L)));
    }

    private ProjectResponse toResponse(ProjectDataModel stored) {
        return ProjectResponse.from(new Project(stored.getId(), stored.getName(), stored.getDescription(),
                stored.getTeamId(), stored.getOwnerId()));
    }
}
