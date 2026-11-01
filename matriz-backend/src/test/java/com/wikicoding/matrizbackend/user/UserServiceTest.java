package com.wikicoding.matrizbackend.user;

import com.wikicoding.matrizbackend.user.domain.User.ProvisioningStatus;
import com.wikicoding.matrizbackend.user.persistence.UserDataModel;
import com.wikicoding.matrizbackend.user.persistence.UserRepository;
import java.util.Optional;
import java.util.UUID;

import com.wikicoding.matrizbackend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Test
    void listsCompletedUsersByEmailWithBoundedPagination() {
        // Arrange
        var users = mock(UserRepository.class);
        var management = new UserService(users);
        var user = new UserDataModel(UUID.randomUUID(), UUID.randomUUID().toString(),
                "member@example.test", UUID.randomUUID(), ProvisioningStatus.COMPLETED, UUID.randomUUID());
        when(users.findByDeletedFalseAndProvisioningStatus(eq(ProvisioningStatus.COMPLETED), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(user)));

        // Act
        var result = management.findAllUsersPagedAndSorted(0, 20);

        // Assert
        assertThat(result.getContent()).extracting("email").containsExactly("member@example.test");
        var page = org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(users).findByDeletedFalseAndProvisioningStatus(eq(ProvisioningStatus.COMPLETED), page.capture());
        assertThat(page.getValue().getSort().getOrderFor("email").isAscending()).isTrue();
        assertThat(page.getValue().getPageSize()).isEqualTo(20);
    }
    private final UserRepository users = mock(UserRepository.class);
    private final UserService management = new UserService(users);

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
