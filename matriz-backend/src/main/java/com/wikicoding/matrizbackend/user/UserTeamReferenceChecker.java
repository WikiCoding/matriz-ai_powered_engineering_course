package com.wikicoding.matrizbackend.user;

import com.wikicoding.matrizbackend.team.TeamReferenceChecker;
import com.wikicoding.matrizbackend.user.persistence.UserRepository;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reports application-user membership references for Team deletion checks. */
@Component
@RequiredArgsConstructor
class UserTeamReferenceChecker implements TeamReferenceChecker {

    private final UserRepository usersRepository;

    @Override
    public boolean hasReferenceToTeamId(UUID teamId) {
        return usersRepository.existsByTeamId(teamId);
    }
}
