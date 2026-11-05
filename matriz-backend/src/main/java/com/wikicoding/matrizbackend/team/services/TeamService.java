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
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/** Creates teams owned by the Team module. */
@Service
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional
    public Team createTeam(String name) {
        log.debug("Operation started: createTeam");
        Team team = new Team(UUID.randomUUID(), name);
        teamRepository.save(new TeamDataModel(team.getId(), team.getName()));
        log.debug("Team creation staged; awaiting transaction commit");
        return team;
    }

    @Transactional(readOnly = true)
    public Page<Team> findAllTeamsPagedAndSorted(int page, int size) {
        log.debug("Operation started: findAllTeamsPagedAndSorted");
        if (page < 0 || size < 1 || size > 100) {
            throw rejected(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        return teamRepository.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))))
                .map(stored -> new Team(stored.getId(), stored.getName()));
    }

    @Transactional(readOnly = true)
    public Team findTeamById(UUID teamId) {
        log.debug("Operation started: findTeamById");
        return teamRepository.findById(teamId)
                .map(stored -> new Team(stored.getId(), stored.getName()))
                .orElseThrow(() -> rejected(HttpStatus.NOT_FOUND, "Team not found"));
    }

    private ResponseStatusException rejected(HttpStatus status, String reason) {
        log.debug("Operation rejected status={} reason={}", status.value(), reason);
        return new ResponseStatusException(status, reason);
    }
}
