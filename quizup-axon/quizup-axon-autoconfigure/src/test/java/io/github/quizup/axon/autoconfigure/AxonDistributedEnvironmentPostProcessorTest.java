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
    void keepsAxonDistributedDefaults() {
        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("axon.distributed.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("spring.cloud.kubernetes.discovery.enabled", Boolean.class)).isFalse();
    }
}
