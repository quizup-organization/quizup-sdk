package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.autoconfigure.metrics.EventPublishMetricsDispatchInterceptor;
import io.github.quizup.axon.autoconfigure.metrics.MessageMetricsHandlerInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.common.Registration;
import org.axonframework.config.Configurer;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Lazy;

/**
 * Enregistre les intercepteurs de métriques d'<b>activité</b> Axon sur les bus : commandes,
 * requêtes et publication d'événements.
 *
 * <p>Calqué sur {@code io.github.quizup.microservice.config.MessageHandlerConfiguration} : les bus
 * distribués sont injectés paresseusement ({@code @Lazy @Qualifier}) et les intercepteurs sont
 * enregistrés en {@code @PostConstruct}, puis dé-enregistrés en {@code @PreDestroy} — pas
 * d'{@code ApplicationReadyEvent} pour ce câblage.</p>
 *
 * <p>Métriques produites : {@code quizup.axon.commands} / {@code quizup.axon.command.duration},
 * {@code quizup.axon.queries} / {@code quizup.axon.query.duration},
 * {@code quizup.axon.events.published}.</p>
 */
@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class, Configurer.class})
@ConditionalOnBean(MeterRegistry.class)
@AutoConfigureAfter(AxonDistributedMetricsAutoConfiguration.class)
public class AxonMetricsInterceptorConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AxonMetricsInterceptorConfiguration.class);

    private final MeterRegistry registry;
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final EventBus eventBus;
    private Registration commandRegistration;
    private Registration queryRegistration;
    private Registration eventRegistration;

    @Autowired
    public AxonMetricsInterceptorConfiguration(
            MeterRegistry registry,
            @Lazy @Qualifier("distributedCommandBus") CommandBus commandBus,
            @Lazy @Qualifier("distributedQueryBus") QueryBus queryBus,
            @Lazy EventBus eventBus) {
        this.registry = registry;
        this.commandBus = commandBus;
        this.queryBus = queryBus;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void registerInterceptors() {
        commandRegistration = commandBus.registerHandlerInterceptor(
                new MessageMetricsHandlerInterceptor<CommandMessage<?>>(
                        registry, "quizup.axon.commands", "quizup.axon.command.duration", null));
        queryRegistration = queryBus.registerHandlerInterceptor(
                new MessageMetricsHandlerInterceptor<QueryMessage<?, ?>>(
                        registry, "quizup.axon.queries", "quizup.axon.query.duration", null));
        eventRegistration = eventBus.registerDispatchInterceptor(
                new EventPublishMetricsDispatchInterceptor(registry));
        logger.info("Axon activity metrics interceptors registered (commandBus, queryBus, eventBus)");
    }

    @PreDestroy
    public void unregisterInterceptors() {
        close(commandRegistration, "commandBus");
        close(queryRegistration, "queryBus");
        close(eventRegistration, "eventBus");
    }

    private void close(Registration registration, String bus) {
        if (registration != null) {
            registration.close();
            logger.info("Axon activity metrics interceptor unregistered from {}", bus);
        }
    }
}
