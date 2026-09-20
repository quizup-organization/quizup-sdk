package io.github.quizup.microservice.autoconfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Injecte les défauts d'observabilité (Actuator / Prometheus) avec la plus basse priorité.
 * <p>
 * Même approche que {@code AxonDistributedEnvironmentPostProcessor} : la property source est
 * ajoutée en dernier ({@code addLast}), donc toute configuration explicite des services
 * (yml ou variables d'environnement) garde la priorité. Cela évite de dupliquer le bloc
 * {@code management:} dans les huit services.
 */
public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "quizupObservabilityDefaultProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        Map<String, Object> defaultProperties = new LinkedHashMap<>();

        // Endpoints Actuator : Prometheus est scrapé in-cluster (déjà permitAll côté sécurité).
        defaultProperties.put("management.endpoints.web.exposure.include", "health,info,metrics,prometheus");
        defaultProperties.put("management.endpoint.health.probes.enabled", "true");

        // Registre Prometheus.
        defaultProperties.put("management.prometheus.metrics.export.enabled", "true");

        // Histogrammes de percentiles : permet à Grafana de calculer p50/p95/p99 côté PromQL.
        defaultProperties.put("management.metrics.distribution.percentiles-histogram.http.server.requests", "true");
        defaultProperties.put("management.metrics.distribution.percentiles-histogram.http.client.requests", "true");

        MutablePropertySources propertySources = environment.getPropertySources();

        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.remove(PROPERTY_SOURCE_NAME);
        }
        propertySources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaultProperties));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
