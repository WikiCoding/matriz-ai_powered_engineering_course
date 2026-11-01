package com.wikicoding.matrizbackend.team.services;

import com.wikicoding.matrizbackend.team.TeamReferenceChecker;
import com.wikicoding.matrizbackend.team.domain.Team;
import com.wikicoding.matrizbackend.team.exceptions.TeamInUseException;
import com.wikicoding.matrizbackend.team.exceptions.TeamNotFoundException;
import com.wikicoding.matrizbackend.team.persistence.TeamDataModel;
import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, renames and deletes teams owned by the Team module. */
@Service
public class TeamService {

    private final TeamRepository teams;
    private final List<TeamReferenceChecker> referenceCheckers;

    public TeamService(TeamRepository teams, List<TeamReferenceChecker> referenceCheckers) {
        this.teams = teams;
        this.referenceCheckers = referenceCheckers;
    }

    @Transactional
    public Team create(String name) {
        Team team = new Team(UUID.randomUUID(), name);
        teams.save(new TeamDataModel(team.getId(), team.getName()));
        return team;
    }

    @Transactional
    public Team update(UUID teamId, String name) {
        requireExisting(teamId);
        Team team = new Team(teamId, name);
        teams.save(new TeamDataModel(team.getId(), team.getName()));
        return team;
    }

    @Transactional
    public void delete(UUID teamId) {
        TeamDataModel team = teams.findById(teamId).orElseThrow(() -> new TeamNotFoundException(teamId));
        if (referenceCheckers.stream().anyMatch(checker -> checker.hasReferenceToTeamId(teamId))) {
            throw new TeamInUseException(teamId);
        }
        try {
            teams.delete(team);
            teams.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new TeamInUseException(teamId, exception);
        }
    }

    private void requireExisting(UUID teamId) {
        if (!teams.existsById(teamId)) {
            throw new TeamNotFoundException(teamId);
        }
    }
}
