package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.autoconfigure.metrics.EventPublishMetricsDispatchInterceptor;
import io.github.quizup.axon.autoconfigure.metrics.MessageMetricsHandlerInterceptor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.ConfigurerModule;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

/**
 * Métriques d'<b>activité</b> Axon (commandes, publication d'événements, traitement d'événements,
 * état des event processors).
 * <p>
 * {@code axon-micrometer} n'instrumente que le {@code queryBus} : le command bus distribué
 * (Spring Cloud) et le bus d'événements (Kafka) échappent au {@code MessageMonitor}. Cette
 * configuration ajoute les compteurs/timers manquants via des intercepteurs Axon :
 * <ul>
 *     <li>{@code quizup.axon.commands} + {@code quizup.axon.command.duration}</li>
 *     <li>{@code quizup.axon.events.published}</li>
 *     <li>{@code quizup.axon.events.processed} + {@code quizup.axon.event.duration}</li>
 *     <li>{@code quizup.axon.event.processor.running|error}</li>
 * </ul>
 * <p>
 * <b>Important</b> : l'interceptor de traitement d'événements est enregistré au moment de la
 * configuration Axon, mais l'enregistrement sur les bus et les jauges de processors sont faits
 * sur {@link ApplicationReadyEvent}. Toucher {@code Configuration.eventProcessors()} pendant
 * {@code onInitialize} force l'initialisation des processors trop tôt et casse le démarrage
 * (NPE {@code Configuration.getComponent}, cycles de beans).
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
        return configurer -> configurer.eventProcessing().registerDefaultHandlerInterceptor(
                (configuration, processorName) -> new MessageMetricsHandlerInterceptor<EventMessage<?>>(
                        registry, "quizup.axon.events.processed", "quizup.axon.event.duration", processorName));
    }

    /**
     * Enregistre les intercepteurs de bus et les jauges de processors une fois le contexte
     * entièrement démarré (évite toute initialisation précoce des composants Axon).
     */
    @Bean
    ApplicationListener<ApplicationReadyEvent> quizupAxonActivityMetricsRegistrar(
            ObjectProvider<MeterRegistry> registries,
            ObjectProvider<EventBus> eventBusProvider,
            ObjectProvider<CommandBus> commandBusProvider,
            ObjectProvider<Configuration> configurationProvider) {
        return event -> {
            try {
                MeterRegistry registry = registries.getIfAvailable();
                if (registry == null) {
                    return;
                }
                EventBus eventBus = eventBusProvider.getIfAvailable();
                if (eventBus != null) {
                    eventBus.registerDispatchInterceptor(new EventPublishMetricsDispatchInterceptor(registry));
                    logger.info("Axon event publish metrics registered");
                }
                CommandBus commandBus = commandBusProvider.getIfAvailable();
                if (commandBus != null) {
                    commandBus.registerHandlerInterceptor(new MessageMetricsHandlerInterceptor<CommandMessage<?>>(
                            registry, "quizup.axon.commands", "quizup.axon.command.duration", null));
                    logger.info("Axon command metrics registered");
                }
                Configuration configuration = configurationProvider.getIfAvailable();
                if (configuration != null) {
                    registerProcessorGauges(configuration, registry);
                }
            } catch (RuntimeException exception) {
                // Les métriques ne doivent jamais empêcher le démarrage du service.
                logger.warn("Could not register Axon activity metrics: {}", exception.getMessage());
            }
        };
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
