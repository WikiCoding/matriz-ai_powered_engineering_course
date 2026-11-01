package com.wikicoding.matrizbackend.user;

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
    private final UserManagement management = new UserManagement(users);

    @Test
    void readsExistingUserWithoutChangingProfile() {
        // Arrange
        UUID id = UUID.randomUUID(), teamId = UUID.randomUUID();
        UserDataModel user = new UserDataModel(id, UUID.randomUUID().toString(),
                "member@example.test", teamId, ProvisioningStatus.COMPLETED, UUID.randomUUID());
        when(users.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(user));

        // Act
        var result = management.requireActive(id);

        // Assert
        assertThat(result.teamId()).isEqualTo(teamId);
        assertThat(result.email()).isEqualTo("member@example.test");
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
        verify(users, never()).flush();
    }

    @Test
    void rejectsUnknownUserWithoutChangingProfiles() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(users.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> management.requireActive(id)).isInstanceOf(ResponseStatusException.class);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
