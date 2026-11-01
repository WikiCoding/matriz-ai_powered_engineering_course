package com.wikicoding.matrizbackend.team.services;

import com.wikicoding.matrizbackend.team.domain.Team;
import com.wikicoding.matrizbackend.team.persistence.TeamDataModel;
import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates teams owned by the Team module. */
@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional
    public Team create(String name) {
        Team team = new Team(UUID.randomUUID(), name);
        teamRepository.save(new TeamDataModel(team.getId(), team.getName()));
        return team;
    }

}
