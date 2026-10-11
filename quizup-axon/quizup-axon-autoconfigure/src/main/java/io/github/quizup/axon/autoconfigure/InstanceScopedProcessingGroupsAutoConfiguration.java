package io.github.quizup.axon.autoconfigure;

import org.axonframework.config.ConfigurerModule;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.messaging.StreamableMessageSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Processing groups **par instance** : chaque réplique d'un service à fan-out temps réel consomme
 * tous les événements (cf. {@link InstanceScopedProcessingGroupsProperties}).
 *
 * <p>Pour chaque groupe de base configuré, un sélecteur de type (prioritaire sur l'annotation
 * {@link ProcessingGroup}) assigne les handlers annotés au nom suffixé `<groupe>-<instanceId>`, et
 * le processor correspondant est enregistré en <b>tracking</b> sur la source streamable par défaut
 * (Kafka) avec un token initial à la <b>queue</b> du flux.</p>
 *
 * <p>Conséquence opérationnelle : les noms de processors sont suffixés (métriques, et
 * {@code token_entry} — un replay ciblé d'un groupe instance-scopé doit supprimer toutes ses lignes
 * suffixées).</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(InstanceScopedProcessingGroupsProperties.class)
public class InstanceScopedProcessingGroupsAutoConfiguration {

    private static final String FALLBACK_INSTANCE_ID = "local";

    @Bean
    public ConfigurerModule instanceScopedProcessingGroupsConfigurer(
            InstanceScopedProcessingGroupsProperties properties,
            Environment environment) {
        List<String> baseGroups = properties.instanceScopedGroups();
        if (baseGroups.isEmpty()) {
            return configurer -> {
                // Aucun groupe instance-scopé : configuration inchangée.
            };
        }
        String instanceId = instanceId(properties, environment);
        return configurer -> configurer.eventProcessing(eventProcessing -> {
            for (String baseGroup : baseGroups) {
                String scopedGroup = baseGroup + "-" + instanceId;
                eventProcessing.assignHandlerTypesMatching(scopedGroup,
                        type -> isAnnotatedWith(type, baseGroup));
                eventProcessing.registerTrackingEventProcessor(scopedGroup);
                eventProcessing.registerTrackingEventProcessorConfiguration(scopedGroup,
                        configuration -> TrackingEventProcessorConfiguration
                                .forSingleThreadedProcessing()
                                .andInitialTrackingToken(StreamableMessageSource::createTailToken));
            }
        });
    }

    private static boolean isAnnotatedWith(Class<?> type, String expectedGroup) {
        ProcessingGroup annotation = AnnotationUtils.findAnnotation(type, ProcessingGroup.class);
        return annotation != null && expectedGroup.equals(annotation.value());
    }

    private static String instanceId(InstanceScopedProcessingGroupsProperties properties,
                                     Environment environment) {
        String configured = properties.instanceId();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return environment.getProperty("HOSTNAME", FALLBACK_INSTANCE_ID);
    }
}
