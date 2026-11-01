package com.wikicoding.matrizbackend.team;

import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TeamLookupAdapter implements TeamLookup {
    private final TeamRepository teams;
    public boolean exists(UUID teamId) { return teams.existsById(teamId); }
}
