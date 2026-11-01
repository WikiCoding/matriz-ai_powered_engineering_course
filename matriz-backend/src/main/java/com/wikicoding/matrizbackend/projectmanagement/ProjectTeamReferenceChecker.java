package com.wikicoding.matrizbackend.projectmanagement;

import com.wikicoding.matrizbackend.projectmanagement.persistence.project.ProjectRepository;
import com.wikicoding.matrizbackend.team.TeamReferenceChecker;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reports project references for Team deletion checks. */
@Component
@RequiredArgsConstructor
class ProjectTeamReferenceChecker implements TeamReferenceChecker {

    private final ProjectRepository projectRepository;

    @Override
    public boolean hasReferenceToTeamId(UUID teamId) {
        return projectRepository.existsByTeamId(teamId);
    }
}
