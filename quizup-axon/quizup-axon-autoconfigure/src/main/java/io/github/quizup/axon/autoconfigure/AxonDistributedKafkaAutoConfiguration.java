package io.github.quizup.axon.autoconfigure;

import org.apache.kafka.clients.admin.NewTopic;
import org.axonframework.config.ConfigurerModule;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.extensions.kafka.KafkaProperties;
import org.axonframework.messaging.StreamableMessageSource;
import org.axonframework.springboot.autoconfig.AxonAutoConfiguration;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Event processing Axon distribué sur Kafka.
 *
 * <p>Les processing groups sont <b>explicites</b> : chaque classe handler doit porter
 * {@link ProcessingGroup} (nom en kebab-case). Le SDK ne déduit plus de groupe par défaut,
 * ce qui permet un replay ciblé par handler (reset du token d'un seul groupe).</p>
 *
 * <p>Le nom du processing group n'est pas utilisé comme consumer group Kafka
 * ({@code StreamableKafkaMessageSource} ouvre des consumers à group id unique) ; il n'est
 * donc pas contraint par Kafka, seulement par l'unicité dans le token store du service.</p>
 */
@AutoConfiguration
@AutoConfigureBefore(AxonAutoConfiguration.class)
public class AxonDistributedKafkaAutoConfiguration {

    private static final String KAFKA_STREAMABLE_SOURCE_BEAN = "streamableKafkaMessageSource";

    /**
     * Câble la source par défaut des event processors sur le flux Kafka streamable et
     * impose la déclaration explicite de {@link ProcessingGroup}.
     *
     * <p>Sans {@code configureDefaultStreamableMessageSource}, un groupe non listé dans
     * {@code axon.eventhandling.processors.*} retomberait silencieusement sur l'event store
     * local ({@code EmbeddedEventStore}) au lieu de Kafka — les handlers cross-service ne
     * verraient alors plus les événements.</p>
     */
    @Bean
    @SuppressWarnings("unchecked")
    public ConfigurerModule axonDefaultEventProcessingConfigurer(ApplicationContext applicationContext) {
        return configurer -> configurer.eventProcessing(eventProcessing -> eventProcessing
                .configureDefaultStreamableMessageSource(configuration ->
                        (StreamableMessageSource<TrackedEventMessage<?>>) applicationContext
                                .getBean(KAFKA_STREAMABLE_SOURCE_BEAN, StreamableMessageSource.class))
                .byDefaultAssignHandlerInstancesTo(handler -> {
                    throw new IllegalStateException(
                            "Event handler [" + handler.getClass().getName() + "] must declare @ProcessingGroup.");
                })
                .byDefaultAssignHandlerTypesTo(type -> {
                    throw new IllegalStateException(
                            "Event handler type [" + type.getName() + "] must declare @ProcessingGroup.");
                }));
    }

    /**
     * Fail-fast si deux classes distinctes déclarent le même {@link ProcessingGroup} : elles
     * fusionneraient silencieusement dans le même processor et le même token.
     */
    @Bean
    public SmartInitializingSingleton processingGroupDuplicateGuard(ApplicationContext applicationContext) {
        return () -> {
            Map<String, Set<Class<?>>> typesByGroup = new HashMap<>();
            for (String beanName : applicationContext.getBeanNamesForAnnotation(ProcessingGroup.class)) {
                Class<?> type = applicationContext.getType(beanName);
                if (type == null) {
                    continue;
                }
                ProcessingGroup annotation = AnnotationUtils.findAnnotation(type, ProcessingGroup.class);
                if (annotation != null) {
                    typesByGroup.computeIfAbsent(annotation.value(), key -> new HashSet<>()).add(type);
                }
            }
            typesByGroup.forEach((group, types) -> {
                if (types.size() > 1) {
                    throw new IllegalStateException(
                            "Duplicate @ProcessingGroup [" + group + "] declared by " + types + ".");
                }
            });
        };
    }

    @Bean
    public NewTopic axonDefaultEventTopic(Environment environment) {
        String topicName = environment.getProperty("axon.kafka.default-topic", KafkaProperties.DEFAULT_TOPIC);
        return new NewTopic(topicName, 3, (short) 1);
    }
}
