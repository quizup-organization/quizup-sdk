package io.github.quizup.axon.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import org.axonframework.tracing.SpanFactory;
import org.axonframework.tracing.opentelemetry.OpenTelemetrySpanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Branche le tracing Axon sur l'{@link OpenTelemetry} géré par Spring Boot (bridge
 * {@code micrometer-tracing-bridge-otel} + exporteur OTLP).
 * <p>
 * Axon fournit sa propre auto-configuration {@code OpenTelemetryAutoConfiguration} qui crée un
 * {@link OpenTelemetrySpanFactory} basé sur {@code GlobalOpenTelemetry} (no-op tant que le SDK
 * OpenTelemetry n'y est pas enregistré). Cette configuration prend le pas
 * ({@code @AutoConfigureBefore}) et construit le {@link SpanFactory} à partir du bean
 * {@link OpenTelemetry} de Spring, afin que les spans Axon (commandes, événements, sagas,
 * deadlines) soient exportés vers le collecteur OTLP.
 * <p>
 * Activée par {@code microservice.observability.tracing.enabled=true} (désactivée par défaut,
 * même bascule que {@code ObservabilityEnvironmentPostProcessor}).
 */
@AutoConfiguration
@ConditionalOnClass({SpanFactory.class, OpenTelemetrySpanFactory.class, OpenTelemetry.class})
@ConditionalOnProperty(prefix = "microservice.observability.tracing", name = "enabled", havingValue = "true")
@AutoConfigureAfter(name = "org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration")
@AutoConfigureBefore(name = "org.axonframework.springboot.autoconfig.OpenTelemetryAutoConfiguration")
public class AxonDistributedTracingAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AxonDistributedTracingAutoConfiguration.class);

    public AxonDistributedTracingAutoConfiguration() {
        logger.info("Axon OpenTelemetry tracing auto-configuration enabled");
    }

    @Bean
    @ConditionalOnMissingBean(SpanFactory.class)
    public SpanFactory spanFactory(OpenTelemetry openTelemetry) {
        logger.info("Registering Axon SpanFactory backed by the Spring-managed OpenTelemetry");
        return OpenTelemetrySpanFactory.builder()
                .tracer(openTelemetry.getTracer("AxonFramework-OpenTelemetry"))
                .contextPropagators(openTelemetry.getPropagators().getTextMapPropagator())
                .build();
    }
}
