package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.autoconfigure.metrics.MessageMetricsHandlerInterceptor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.ConfigurerModule;
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
 * Métriques d'<b>activité</b> Axon (traitement d'événements et état des event processors).
 *
 * <ul>
 *     <li>{@code quizup.axon.events.processed} + {@code quizup.axon.event.duration} — interceptor
 *     de traitement d'événements, enregistré via {@link ConfigurerModule} (temps de configuration Axon) ;</li>
 *     <li>{@code quizup.axon.event.processor.running|error} — jauges d'état des processors.</li>
 * </ul>
 *
 * <p>Les intercepteurs des <b>bus</b> (commandes, requêtes, publication d'événements) ne sont
 * <b>pas</b> gérés ici : ils sont enregistrés de façon déclarative par
 * {@link AxonMetricsInterceptorConfiguration} (modèle {@code MessageHandlerConfiguration}).</p>
 *
 * <p><b>Important</b> : les jauges de processors sont enregistrées sur
 * {@link ApplicationReadyEvent}, car toucher {@code Configuration.eventProcessors()} pendant
 * {@code onInitialize} force l'initialisation des processors trop tôt et casse le démarrage
 * (NPE {@code Configuration.getComponent}, cycles de beans).</p>
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
     * Enregistre les jauges d'état des event processors une fois le contexte entièrement démarré
     * (évite toute initialisation précoce des composants Axon).
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> quizupAxonEventProcessorGauges(
            ObjectProvider<MeterRegistry> registries,
            ObjectProvider<Configuration> configurationProvider) {
        return event -> {
            try {
                MeterRegistry registry = registries.getIfAvailable();
                Configuration configuration = configurationProvider.getIfAvailable();
                if (registry != null && configuration != null) {
                    registerProcessorGauges(configuration, registry);
                }
            } catch (RuntimeException exception) {
                // Les métriques ne doivent jamais empêcher le démarrage du service.
                logger.warn("Could not register Axon event processor gauges: {}", exception.getMessage());
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
