package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.console.QuizupAxonDlqProperties;
import io.github.quizup.axon.console.QuizupDeadLetterQueueProviderConfigurerModule;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.deadletter.jpa.JpaSequencedDeadLetterQueue;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.axonframework.serialization.Serializer;
import org.axonframework.springboot.EventProcessorProperties;
import org.axonframework.springboot.util.DeadLetterQueueProviderConfigurerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

/**
 * Active la <b>Dead-Letter Queue Axon</b> par défaut pour les processing groups du SDK
 * (cf. {@link QuizupDeadLetterQueueProviderConfigurerModule} et {@link QuizupAxonDlqProperties}) :
 * un événement dont le handler échoue ne bloque plus le segment du processor.
 *
 * <p>Le bean {@link DeadLetterQueueProviderConfigurerModule} d'Axon (créé par
 * {@code JpaAutoConfiguration}, no-op sans {@code axon.eventhandling.processors.*.dlq.enabled})
 * est remplacé : cette auto-configuration passe <b>après</b> Hibernate (pour évaluer
 * {@code EntityManagerFactory}) et <b>avant</b> {@code JpaAutoConfiguration} (pour gagner la
 * condition {@code @ConditionalOnMissingBean} d'Axon).</p>
 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration")
@AutoConfigureBefore(name = "org.axonframework.springboot.autoconfig.JpaAutoConfiguration")
@ConditionalOnClass(name = {
        "jakarta.persistence.EntityManagerFactory",
        "org.axonframework.eventhandling.deadletter.jpa.JpaSequencedDeadLetterQueue",
        "org.axonframework.springboot.util.DeadLetterQueueProviderConfigurerModule"
})
@ConditionalOnBean(type = "jakarta.persistence.EntityManagerFactory")
@EnableConfigurationProperties(QuizupAxonDlqProperties.class)
public class AxonConsoleDlqAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AxonConsoleDlqAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(DeadLetterQueueProviderConfigurerModule.class)
    public QuizupDeadLetterQueueProviderConfigurerModule quizupDeadLetterQueueProviderConfigurerModule(
            QuizupAxonDlqProperties dlqProperties,
            InstanceScopedProcessingGroupsProperties instanceScopedProperties,
            EventProcessorProperties axonProperties,
            EntityManagerProvider entityManagerProvider,
            TransactionManager transactionManager,
            Serializer genericSerializer,
            @Qualifier("eventSerializer") Serializer eventSerializer) {
        Function<String, Function<Configuration, SequencedDeadLetterQueue<EventMessage<?>>>> provider =
                processingGroup -> configuration -> buildQueue(
                        processingGroup, dlqProperties, entityManagerProvider,
                        transactionManager, genericSerializer, eventSerializer);
        logger.info("QuizUp Axon DLQ auto-configuration enabled");
        return new QuizupDeadLetterQueueProviderConfigurerModule(
                dlqProperties, instanceScopedProperties.instanceScopedGroups(), axonProperties, provider);
    }

    private static SequencedDeadLetterQueue<EventMessage<?>> buildQueue(
            String processingGroup,
            QuizupAxonDlqProperties properties,
            EntityManagerProvider entityManagerProvider,
            TransactionManager transactionManager,
            Serializer genericSerializer,
            Serializer eventSerializer) {
        JpaSequencedDeadLetterQueue.Builder<EventMessage<?>> builder =
                JpaSequencedDeadLetterQueue.<EventMessage<?>>builder()
                        .processingGroup(processingGroup)
                        .entityManagerProvider(entityManagerProvider)
                        .transactionManager(transactionManager)
                        .genericSerializer(genericSerializer)
                        .eventSerializer(eventSerializer);
        if (properties.maxSequences() != null) {
            builder.maxSequences(properties.maxSequences());
        }
        if (properties.maxSequenceSize() != null) {
            builder.maxSequenceSize(properties.maxSequenceSize());
        }
        if (properties.claimDuration() != null) {
            builder.claimDuration(properties.claimDuration());
        }
        return builder.build();
    }
}
