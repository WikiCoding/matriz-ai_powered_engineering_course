package com.wikicoding.matrizbackend.user;

import com.wikicoding.matrizbackend.projectmanagement.ProjectOwnerLookup;
import com.wikicoding.matrizbackend.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProjectOwnerLookupAdapter implements ProjectOwnerLookup {
    private final UserService users;

    @Override
    public java.util.UUID requireActiveOwnerId(String subject) {
        return users.requireActiveSubject(subject).id();
    }
}
