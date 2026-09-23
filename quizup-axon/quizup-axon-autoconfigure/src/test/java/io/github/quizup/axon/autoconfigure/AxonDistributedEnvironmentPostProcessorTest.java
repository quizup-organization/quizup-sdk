package io.github.quizup.axon.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AxonDistributedEnvironmentPostProcessorTest {

    private final StandardEnvironment environment = new StandardEnvironment();
    private final AxonDistributedEnvironmentPostProcessor postProcessor =
            new AxonDistributedEnvironmentPostProcessor();

    @Test
    void includesNotReadyAddressesSoTheLocalPodIsDiscoverableAtStartup() {
        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(
                "spring.cloud.kubernetes.discovery.include-not-ready-addresses", Boolean.class))
                .isTrue();
    }

    @Test
    void disablesAxonIqConsoleMessageSystemPropertyByDefault() {
        String previous = System.getProperty("disable-axoniq-console-message");
        System.clearProperty("disable-axoniq-console-message");
        try {
            postProcessor.postProcessEnvironment(environment, new SpringApplication());

            assertThat(System.getProperty("disable-axoniq-console-message")).isEqualTo("true");
        } finally {
            if (previous == null) {
                System.clearProperty("disable-axoniq-console-message");
            } else {
                System.setProperty("disable-axoniq-console-message", previous);
            }
        }
    }

    @Test
    void keepsAxonDistributedDefaults() {
        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("axon.distributed.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.cloud.kubernetes.discovery.enabled", Boolean.class)).isFalse();
    }
}
