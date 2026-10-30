package com.wikicoding.matrizbackend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleStructureTests {

    @Test
    void detectsApprovedModulesAndVerifiesBoundaries() {
        var modules = ApplicationModules.of(MatrizBackendApplication.class);

        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrder("user", "team", "projectmanagement");
        modules.verify();
    }
}
