package com.wikicoding.matrizbackend.team.services;

import com.wikicoding.matrizbackend.team.persistence.TeamDataModel;
import com.wikicoding.matrizbackend.team.persistence.TeamRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamServiceTest {

    @Test
    void listsTeamsAlphabeticallyWithBoundedPagination() {
        // Arrange
        TeamRepository repository = mock(TeamRepository.class);
        TeamService service = new TeamService(repository);
        when(repository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(new TeamDataModel(UUID.randomUUID(), "Platform"))));

        // Act
        var result = service.findAllTeamsPagedAndSorted(0, 20);

        // Assert
        assertThat(result.getContent()).extracting("name").containsExactly("Platform");
        var page = org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).findAll(page.capture());
        assertThat(page.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
        assertThat(page.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void createsTeamWithGeneratedIdAndProvidedName() {
        // Arrange
        TeamRepository repository = mock(TeamRepository.class);
        TeamService service = new TeamService(repository);
        ArgumentCaptor<TeamDataModel> savedTeam = ArgumentCaptor.forClass(TeamDataModel.class);

        // Act
        var team = service.createTeam("Platform");

        // Assert
        verify(repository).save(savedTeam.capture());
        assertThat(team.getId()).isEqualTo(savedTeam.getValue().getId());
        assertThat(team.getName()).isEqualTo("Platform");
    }

}
