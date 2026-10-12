package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.console.ConsoleAgent;
import io.github.quizup.axon.console.ConsoleController;
import io.github.quizup.axon.console.ConsoleEventStoreController;
import io.github.quizup.axon.console.ConsoleEventStoreService;
import io.github.quizup.axon.console.ConsoleState;
import io.github.quizup.axon.console.QuizupAxonConsoleProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Console d'exploitation Axon (équivalent fonctionnel open source de la Axoniq Platform) :
 *
 * <ul>
 *     <li>{@link ConsoleState} — lecture de l'état local (processors, segments, DLQ) ;</li>
 *     <li>{@link ConsoleAgent} — push périodique M2M vers {@code quizup-console}
 *     ({@code quizup.axon.console.enabled=true}, défaut {@code false}) ;</li>
 *     <li>{@link ConsoleController} — API interne {@code /internal/console/**} (jamais ingressed,
 *     JWT M2M requis) consommée par le service console ;</li>
 *     <li>jauges Micrometer {@code quizup.axon.console.*} (DLQ, caught-up, replay).</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass({Configuration.class, RestController.class})
@EnableConfigurationProperties(QuizupAxonConsoleProperties.class)
public class AxonConsoleAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AxonConsoleAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ConsoleState consoleState(ObjectProvider<Configuration> configurationProvider,
                                     Environment environment,
                                     ObjectProvider<BuildProperties> buildProperties,
                                     QuizupAxonConsoleProperties properties) {
        return new ConsoleState(configurationProvider, environment, buildProperties, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "quizup.axon.console", name = "enabled", havingValue = "true")
    public ConsoleAgent consoleAgent(ConsoleState state,
                                     ObjectProvider<RestTemplate> restTemplateProvider,
                                     QuizupAxonConsoleProperties properties) {
        return new ConsoleAgent(state, restTemplateProvider, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "quizup.axon.console", name = "internal-api-enabled",
            havingValue = "true", matchIfMissing = true)
    public ConsoleController consoleController(ConsoleState state) {
        logger.info("Axon console internal API enabled (/internal/console/**)");
        return new ConsoleController(state);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication
    @ConditionalOnClass(JdbcTemplate.class)
    @ConditionalOnProperty(prefix = "quizup.axon.console", name = "internal-api-enabled",
            havingValue = "true", matchIfMissing = true)
    public ConsoleEventStoreService consoleEventStoreService(
            ObjectProvider<DataSource> dataSourceProvider,
            ObjectProvider<EventStore> eventStoreProvider) {
        return new ConsoleEventStoreService(dataSourceProvider, eventStoreProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication
    @ConditionalOnClass(JdbcTemplate.class)
    @ConditionalOnProperty(prefix = "quizup.axon.console", name = "internal-api-enabled",
            havingValue = "true", matchIfMissing = true)
    public ConsoleEventStoreController consoleEventStoreController(ConsoleEventStoreService service) {
        return new ConsoleEventStoreController(service);
    }

    /**
     * Jauges d'exploitation, enregistrées sur {@link ApplicationReadyEvent} (les processors ne
     * doivent pas être touchés pendant l'initialisation Axon).
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public ApplicationListener<ApplicationReadyEvent> quizupAxonConsoleMetrics(
            ObjectProvider<MeterRegistry> registries,
            ObjectProvider<ConsoleState> stateProvider) {
        return event -> {
            try {
                MeterRegistry registry = registries.getIfAvailable();
                ConsoleState state = stateProvider.getIfAvailable();
                if (registry != null && state != null) {
                    registerConsoleGauges(state, registry);
                }
            } catch (RuntimeException exception) {
                logger.warn("Could not register Axon console gauges: {}", exception.getMessage());
            }
        };
    }

    private void registerConsoleGauges(ConsoleState state, MeterRegistry registry) {
        Map<String, EventProcessor> processors = state.processors();
        processors.forEach((name, processor) -> {
            Gauge.builder("quizup.axon.console.processor.caught_up", processor,
                            p -> ConsoleState.caughtUp(p) ? 1 : 0)
                    .description("Event processor à jour sur tous ses segments")
                    .tag("processor", name)
                    .register(registry);
            Gauge.builder("quizup.axon.console.processor.replaying", processor,
                            p -> ConsoleState.replaying(p) ? 1 : 0)
                    .description("Event processor en cours de replay")
                    .tag("processor", name)
                    .register(registry);
            state.deadLetterQueue(name).ifPresent(queue -> {
                Gauge.builder("quizup.axon.console.dlq.size", queue, AxonConsoleAutoConfiguration::dlqSize)
                        .description("Nombre de messages en dead-letter queue")
                        .tag("processor", name)
                        .register(registry);
                Gauge.builder("quizup.axon.console.dlq.sequences", queue, AxonConsoleAutoConfiguration::dlqSequences)
                        .description("Nombre de séquences en dead-letter queue")
                        .tag("processor", name)
                        .register(registry);
            });
        });
        logger.info("Axon console gauges registered ({} processors)", processors.size());
    }

    private static double dlqSize(SequencedDeadLetterQueue<EventMessage<?>> queue) {
        try {
            return queue.size();
        } catch (RuntimeException exception) {
            return Double.NaN;
        }
    }

    private static double dlqSequences(SequencedDeadLetterQueue<EventMessage<?>> queue) {
        try {
            return queue.amountOfSequences();
        } catch (RuntimeException exception) {
            return Double.NaN;
        }
    }
}
