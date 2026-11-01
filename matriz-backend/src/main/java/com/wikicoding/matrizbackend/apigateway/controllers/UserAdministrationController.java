package com.wikicoding.matrizbackend.apigateway.controllers;

import com.wikicoding.matrizbackend.apigateway.dtos.TeamIds;
import com.wikicoding.matrizbackend.user.UserAccounts;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class UserAdministrationController {
    private final UserAccounts userService;

    @GetMapping("/team-memberships")
    public TeamIds membership(@PathVariable UUID userId) {
        return new TeamIds(List.of(userService.requireActiveTeamId(userId)));
    }
}
