package io.github.quizup.microservice.autoconfigure;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityAutoConfigurationTest {

    @Test
    void environmentPostProcessorExposesPrometheusByDefault() {
        MockEnvironment environment = new MockEnvironment();

        new ObservabilityEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .contains("prometheus")
                .contains("health");
        assertThat(environment.getProperty("management.prometheus.metrics.export.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("management.endpoint.health.probes.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("ecs");
    }

    @Test
    void environmentPostProcessorDisablesTracingByDefault() {
        MockEnvironment environment = new MockEnvironment();

        new ObservabilityEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.tracing.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("management.otlp.tracing.endpoint")).isNull();
    }

    @Test
    void environmentPostProcessorEnablesTracingWhenRequested() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("microservice.observability.tracing.enabled", "true");

        new ObservabilityEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.tracing.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("management.tracing.sampling.probability")).isEqualTo("0.1");
        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .isEqualTo("http://otel-collector.monitoring.svc.cluster.local:4318/v1/traces");
    }

    @Test
    void environmentPostProcessorDoesNotOverrideExplicitConfiguration() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("management.endpoints.web.exposure.include", "health");

        new ObservabilityEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
    }

    @Test
    void commonTagsAreAppliedToEveryMeter() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.application.name", "quizup-test");
        environment.setActiveProfiles("prod");

        ObjectProvider<BuildProperties> buildProperties =
                new DefaultListableBeanFactory().getBeanProvider(BuildProperties.class);

        MeterRegistryCustomizer<MeterRegistry> customizer =
                new ObservabilityAutoConfiguration().quizupCommonTagsCustomizer(environment, buildProperties);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        customizer.customize(registry);
        Gauge.builder("quizup.dummy", () -> 1.0).register(registry);

        assertThat(registry.get("quizup.dummy").gauge().getId().getTags())
                .contains(Tag.of("application", "quizup-test"))
                .contains(Tag.of("environment", "prod"))
                .contains(Tag.of("version", "unknown"));
    }
}
