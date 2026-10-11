package com.example.documentqa.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseCredentialConfigTest {
    private MockEnvironment environment() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        for (var source : new YamlPropertySourceLoader().load(
                "application", new ClassPathResource("application.yml"))) {
            environment.getPropertySources().addLast(source);
        }
        return environment;
    }

    @Test void missingRuntimePasswordCannotSelectACommittedFallback() throws IOException {
        MockEnvironment environment = environment();
        assertThrows(IllegalArgumentException.class,
            () -> environment.getRequiredProperty("spring.datasource.password"));
    }

    @Test void runtimePasswordIsPassedThroughWithoutSubstitution() throws IOException {
        MockEnvironment environment = environment().withProperty("POSTGRES_PASSWORD", "fixture-runtime-value");
        assertEquals("fixture-runtime-value", environment.getRequiredProperty("spring.datasource.password"));
    }
}
