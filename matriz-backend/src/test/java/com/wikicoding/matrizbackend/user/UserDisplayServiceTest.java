package com.wikicoding.matrizbackend.user;

import com.wikicoding.matrizbackend.user.services.UserDisplayService;
import com.wikicoding.matrizbackend.user.services.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDisplayServiceTest {
    @Test
    void offersOnlyActiveEngineersInTheProjectTeam() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        UUID engineerId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        var users = mock(UserService.class);
        when(users.findActiveTeamMembers(teamId)).thenReturn(List.of(
                new UserService.ManagedUser(engineerId, "engineer-subject", "engineer@example.test", teamId),
                new UserService.ManagedUser(managerId, "manager-subject", "manager@example.test", teamId)));
        var keycloak = WebClient.builder().exchangeFunction(request -> {
            String body = request.url().getPath().endsWith("/token")
                    ? "{\"access_token\":\"service-token\"}"
                    : "[{\"id\":\"engineer-subject\",\"firstName\":\"Ana\",\"lastName\":\"Silva\"},"
                    + "{\"id\":\"other-team-subject\",\"firstName\":\"Other\"}]";
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(body).build());
        }).build();
        var service = new UserDisplayService(users, keycloak, "matriz", "provisioning", "secret");

        // Act
        var options = service.eligibleForTeam(teamId);

        // Assert
        assertThat(options).containsExactly(new com.wikicoding.matrizbackend.projectmanagement.AssigneeOptionsLookup.Option(
                engineerId, "Ana Silva"));
    }
}
