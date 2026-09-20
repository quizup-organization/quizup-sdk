package io.github.quizup.microservice.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration automatique d'observabilité.
 * <p>
 * Ajoute les tags communs partagés par tous les microservices QuizUp afin que les séries
 * Prometheus soient agrégeables/filtrées uniformément dans Grafana :
 * <ul>
 *     <li>{@code application} — {@code spring.application.name}</li>
 *     <li>{@code environment} — profil Spring actif (prod/local/…)</li>
 *     <li>{@code version} — version du build si {@code build-info} est présent</li>
 * </ul>
 * Le registre Prometheus (endpoint {@code /actuator/prometheus}) et les défauts Actuator sont
 * apportés par {@code micrometer-registry-prometheus} et
 * {@link ObservabilityEnvironmentPostProcessor}.
 * <p>
 * Activé par défaut, désactivable avec {@code microservice.observability.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "microservice.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityAutoConfiguration.class);

    public ObservabilityAutoConfiguration() {
        logger.info("Observability auto-configuration enabled");
    }

    @Bean
    @ConditionalOnMissingBean(name = "quizupCommonTagsCustomizer")
    public MeterRegistryCustomizer<MeterRegistry> quizupCommonTagsCustomizer(
            Environment environment,
            ObjectProvider<BuildProperties> buildProperties) {

        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("application", environment.getProperty("spring.application.name", "unknown")));

        String[] profiles = environment.getActiveProfiles();
        tags.add(Tag.of("environment", profiles.length > 0 ? profiles[0] : "default"));

        BuildProperties build = buildProperties.getIfAvailable();
        tags.add(Tag.of("version", build != null ? build.getVersion()
                : environment.getProperty("info.app.version", "unknown")));

        logger.info("Registering common metric tags: {}", tags);
        return registry -> registry.config().commonTags(tags);
    }
}
