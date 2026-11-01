package com.wikicoding.matrizbackend.projectmanagement.dtos.tasks;

import com.wikicoding.matrizbackend.projectmanagement.domain.task.Task;
import java.util.List;
import org.springframework.data.domain.Page;

public record TaskPageResponse(List<TaskResponse> content, long totalElements, int totalPages, int number, int size) {
    public static TaskPageResponse from(Page<Task> page) {
        return new TaskPageResponse(page.getContent().stream().map(TaskResponse::from).toList(),
                page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
