package com.wikicoding.matrizbackend.projectmanagement.dtos.projects;

import java.util.List;
import java.util.Map;

public record DashboardResponse(List<ProjectResponse> projects, Map<String, Long> taskCounts) {}
