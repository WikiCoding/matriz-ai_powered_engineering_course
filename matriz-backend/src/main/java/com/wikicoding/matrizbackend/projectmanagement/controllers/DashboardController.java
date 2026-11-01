package com.wikicoding.matrizbackend.projectmanagement.controllers;

import com.wikicoding.matrizbackend.projectmanagement.dtos.projects.DashboardResponse;
import com.wikicoding.matrizbackend.projectmanagement.services.DashboardService;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboardService(JwtAuthenticationToken principal) {
        boolean administrator = principal.getToken().getClaim("realm_access") instanceof Map<?, ?> access
                && access.get("roles") instanceof Collection<?> roles
                && roles.contains("ADMINISTRATOR");

        return dashboardService.getDashboard(administrator);
    }
}
