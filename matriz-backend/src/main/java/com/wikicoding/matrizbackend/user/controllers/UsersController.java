package com.wikicoding.matrizbackend.user.controllers;

import com.wikicoding.matrizbackend.user.services.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {
    private final UserService userService;

    @GetMapping
    public UserPage findAllUsers(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        var result = userService.findAllUsersPagedAndSorted(page, size);
        return new UserPage(result.getContent().stream()
                .map(user -> new UserItem(user.id(), user.email(), user.teamId())).toList(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    public record UserItem(UUID id, String email, UUID teamId) {}
    public record UserPage(List<UserItem> content, long totalElements, int totalPages, int number, int size) {}
}
