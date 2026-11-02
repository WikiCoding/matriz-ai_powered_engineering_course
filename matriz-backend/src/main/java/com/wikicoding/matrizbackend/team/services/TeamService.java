package com.wikicoding.matrizbackend.team.services;

import com.wikicoding.matrizbackend.team.domain.Team;
import com.wikicoding.matrizbackend.team.persistence.TeamDataModel;
import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    public Team createTeam(String name) {
        Team team = new Team(UUID.randomUUID(), name);
        teamRepository.save(new TeamDataModel(team.getId(), team.getName()));
        return team;
    }

    @Transactional(readOnly = true)
    public Page<Team> findAllTeamsPagedAndSorted(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        return teamRepository.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))))
                .map(stored -> new Team(stored.getId(), stored.getName()));
    }

    @Transactional(readOnly = true)
    public Team findTeamById(UUID teamId) {
        return teamRepository.findById(teamId)
                .map(stored -> new Team(stored.getId(), stored.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }

}
