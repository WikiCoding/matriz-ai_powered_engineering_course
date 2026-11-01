package com.wikicoding.matrizbackend.projectmanagement;

import com.wikicoding.matrizbackend.projectmanagement.persistence.TaskAssignmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AssignmentCleanupAdapter implements UserAssignmentCleanup {
    private final TaskAssignmentRepository assignments;

    @Override
    public void removeForUser(UUID userId) {
        assignments.deleteByUserId(userId);
        assignments.flush();
    }
}
