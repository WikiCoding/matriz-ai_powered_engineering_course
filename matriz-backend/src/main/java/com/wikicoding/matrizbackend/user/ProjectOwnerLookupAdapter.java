package com.wikicoding.matrizbackend.user;

import com.wikicoding.matrizbackend.projectmanagement.ProjectOwnerLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProjectOwnerLookupAdapter implements ProjectOwnerLookup {
    private final UserManagement users;

    @Override
    public java.util.UUID requireActiveOwnerId(String subject) {
        return users.requireActiveSubject(subject).id();
    }
}
