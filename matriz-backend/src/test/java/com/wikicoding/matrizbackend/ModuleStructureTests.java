package com.wikicoding.matrizbackend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleStructureTests {

    @Test
    void detectsApprovedModulesAndVerifiesBoundaries() {
        // Arrange: inspect the production application.
        var modules = ApplicationModules.of(MatrizBackendApplication.class);

        // Act
        modules.verify();

        // Assert
        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrder("user", "team", "projectmanagement", "edge");
    }
}
