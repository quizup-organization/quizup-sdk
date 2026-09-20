package io.github.quizup.axon.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import org.axonframework.tracing.SpanFactory;
import org.axonframework.tracing.opentelemetry.OpenTelemetrySpanFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AxonDistributedTracingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AxonDistributedTracingAutoConfiguration.class))
            .withBean(OpenTelemetry.class, OpenTelemetry::noop);

    @Test
    void createsSpanFactoryWhenTracingEnabled() {
        runner.withPropertyValues("microservice.observability.tracing.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(SpanFactory.class)
                        .getBean(SpanFactory.class)
                        .isInstanceOf(OpenTelemetrySpanFactory.class));
    }

    @Test
    void backsOffWhenTracingDisabled() {
        runner.run(context -> assertThat(context).doesNotHaveBean(SpanFactory.class));
    }
}
