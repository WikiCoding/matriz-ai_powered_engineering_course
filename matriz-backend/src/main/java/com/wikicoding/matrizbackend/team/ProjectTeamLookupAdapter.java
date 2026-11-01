package com.wikicoding.matrizbackend.team;

import com.wikicoding.matrizbackend.projectmanagement.ProjectTeamLookup;
import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProjectTeamLookupAdapter implements ProjectTeamLookup {
    private final TeamRepository teamRepository;

    @Override
    public boolean exists(UUID teamId) {
        return teamRepository.existsById(teamId);
    }
}
