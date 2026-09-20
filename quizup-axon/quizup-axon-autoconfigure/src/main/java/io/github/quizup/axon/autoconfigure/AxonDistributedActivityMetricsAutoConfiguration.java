package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.autoconfigure.metrics.EventPublishMetricsDispatchInterceptor;
import io.github.quizup.axon.autoconfigure.metrics.MessageMetricsHandlerInterceptor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.ConfigurerModule;
import org.axonframework.eventhandling.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Métriques d'<b>activité</b> Axon (commandes, publication d'événements, traitement d'événements,
 * état des event processors).
 * <p>
 * {@code axon-micrometer} n'instrumente que le {@code queryBus} : le command bus distribué
 * (Spring Cloud) et le bus d'événements (Kafka) échappent au {@code MessageMonitor}. Cette
 * configuration ajoute les compteurs/timers manquants via des intercepteurs Axon, pour
 * reproduire les vues d'activité d'Axon Server :
 * <ul>
 *     <li>{@code quizup.axon.commands} + {@code quizup.axon.command.duration} (par commande, résultat)</li>
 *     <li>{@code quizup.axon.events.published} (par type d'événement)</li>
 *     <li>{@code quizup.axon.events.processed} + {@code quizup.axon.event.duration} (par processor)</li>
 *     <li>{@code quizup.axon.event.processor.running|error} (jauges par processor)</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class, Configurer.class, ConfigurerModule.class})
@ConditionalOnBean(MeterRegistry.class)
@AutoConfigureAfter(AxonDistributedMetricsAutoConfiguration.class)
public class AxonDistributedActivityMetricsAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(AxonDistributedActivityMetricsAutoConfiguration.class);

    public AxonDistributedActivityMetricsAutoConfiguration() {
        logger.info("Axon activity metrics auto-configuration enabled");
    }

    @Bean
    @ConditionalOnMissingBean(name = "quizupAxonActivityMetricsConfigurerModule")
    public ConfigurerModule quizupAxonActivityMetricsConfigurerModule(MeterRegistry registry) {
        return configurer -> {
            // Traitement des événements : interceptor appliqué à tous les event processors.
            configurer.eventProcessing().registerDefaultHandlerInterceptor(
                    (configuration, processorName) -> new MessageMetricsHandlerInterceptor<EventMessage<?>>(
                            registry, "quizup.axon.events.processed", "quizup.axon.event.duration", processorName));

            configurer.onInitialize(configuration -> {
                registerEventPublishMetrics(configuration, registry);
                registerCommandMetrics(configuration, registry);
                registerProcessorGauges(configuration, registry);
            });
        };
    }

    private void registerEventPublishMetrics(Configuration configuration, MeterRegistry registry) {
        try {
            configuration.eventBus().registerDispatchInterceptor(
                    new EventPublishMetricsDispatchInterceptor(registry));
            logger.info("Axon event publish metrics registered");
        } catch (RuntimeException exception) {
            logger.warn("Could not register event publish metrics: {}", exception.getMessage());
        }
    }

    private void registerCommandMetrics(Configuration configuration, MeterRegistry registry) {
        try {
            configuration.commandBus().registerHandlerInterceptor(
                    new MessageMetricsHandlerInterceptor<CommandMessage<?>>(
                            registry, "quizup.axon.commands", "quizup.axon.command.duration", null));
            logger.info("Axon command metrics registered");
        } catch (RuntimeException exception) {
            logger.warn("Could not register command metrics: {}", exception.getMessage());
        }
    }

    private void registerProcessorGauges(Configuration configuration, MeterRegistry registry) {
        configuration.eventProcessingConfiguration().eventProcessors().forEach((name, processor) -> {
            Gauge.builder("quizup.axon.event.processor.running", processor, p -> p.isRunning() ? 1 : 0)
                    .description("Event processor démarré")
                    .tag("processor", name)
                    .register(registry);
            Gauge.builder("quizup.axon.event.processor.error", processor, p -> p.isError() ? 1 : 0)
                    .description("Event processor en erreur")
                    .tag("processor", name)
                    .register(registry);
        });
        logger.info("Axon event processor gauges registered ({})",
                configuration.eventProcessingConfiguration().eventProcessors().size());
    }
}
