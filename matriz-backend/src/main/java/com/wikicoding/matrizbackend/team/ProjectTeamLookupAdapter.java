package com.wikicoding.matrizbackend.team;

import com.wikicoding.matrizbackend.projectmanagement.ProjectTeamLookup;
import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProjectTeamLookupAdapter implements ProjectTeamLookup {
    private final TeamRepository teams;

    @Override
    public boolean exists(java.util.UUID teamId) {
        return teams.existsById(teamId);
    }
}
