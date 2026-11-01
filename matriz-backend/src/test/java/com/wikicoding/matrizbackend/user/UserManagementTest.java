package com.wikicoding.matrizbackend.user;

import com.wikicoding.matrizbackend.projectmanagement.UserAssignmentCleanup;
import com.wikicoding.matrizbackend.team.TeamLookup;
import com.wikicoding.matrizbackend.user.domain.User.ProvisioningStatus;
import com.wikicoding.matrizbackend.user.persistence.UserDataModel;
import com.wikicoding.matrizbackend.user.persistence.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserManagementTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserAssignmentCleanup assignments = mock(UserAssignmentCleanup.class);
    private final TeamLookup teams = mock(TeamLookup.class);
    private final UserManagement management = new UserManagement(users, assignments, teams);

    @Test
    void movingTeamRemovesAssignmentsAndChangesMembership() {
        // Arrange
        UUID id = UUID.randomUUID(), oldTeam = UUID.randomUUID(), newTeam = UUID.randomUUID();
        UserDataModel user = profile(id, oldTeam);
        when(teams.exists(newTeam)).thenReturn(true);
        when(users.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(user));

        // Act
        var result = management.moveToTeam(id, newTeam);

        // Assert
        assertThat(result.teamId()).isEqualTo(newTeam);
        assertThat(user.getTeamId()).isEqualTo(newTeam);
        var order = inOrder(assignments, users);
        order.verify(assignments).removeForUser(id);
        order.verify(users).flush();
    }

    @Test
    void invalidTeamDoesNotChangeUserOrAssignments() {
        // Arrange
        UUID id = UUID.randomUUID(), unknownTeam = UUID.randomUUID();
        when(teams.exists(unknownTeam)).thenReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> management.moveToTeam(id, unknownTeam))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(users, assignments);
    }

    @Test
    void deletingUserRemovesAssignmentsAndAnonymizesProfile() {
        // Arrange
        UUID id = UUID.randomUUID();
        UserDataModel user = profile(id, UUID.randomUUID());
        when(users.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(user));

        // Act
        management.deleteLocal(id);

        // Assert
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getEmail()).startsWith("deleted-");
        assertThat(user.getKeycloakSubject()).isNull();
        assertThat(user.getTeamId()).isNull();
        verify(assignments).removeForUser(id);
        verify(users).flush();
    }

    private UserDataModel profile(UUID id, UUID teamId) {
        return new UserDataModel(id, UUID.randomUUID().toString(), "member@example.test", teamId,
                ProvisioningStatus.COMPLETED, UUID.randomUUID());
    }
}
