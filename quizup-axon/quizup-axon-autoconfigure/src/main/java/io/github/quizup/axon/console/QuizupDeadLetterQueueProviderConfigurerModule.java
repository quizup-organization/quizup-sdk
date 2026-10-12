package io.github.quizup.axon.console;

import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.axonframework.springboot.EventProcessorProperties;
import org.axonframework.springboot.util.DeadLetterQueueProviderConfigurerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * Remplace le module DLQ no-op d'Axon ({@code DeadLetterQueueProviderConfigurerModule} de
 * {@code JpaAutoConfiguration}, qui n'active la DLQ que sur les groupes listés dans
 * {@code axon.eventhandling.processors.*.dlq.enabled}) par une activation <b>par défaut</b> de la
 * DLQ sur tous les groupes kebab-case, avec exclusions (cf. {@link QuizupAxonDlqProperties}).
 *
 * <p>Le bean est déclaré par {@code AxonConsoleDlqAutoConfiguration} sous le type
 * {@link DeadLetterQueueProviderConfigurerModule} : le bean Axon (annoté
 * {@code @ConditionalOnMissingBean}) se retire, ce qui évite deux enregistrements concurrents du
 * provider (le dernier gagnant serait indéterministe).</p>
 */
public class QuizupDeadLetterQueueProviderConfigurerModule extends DeadLetterQueueProviderConfigurerModule {

    private static final Logger logger =
            LoggerFactory.getLogger(QuizupDeadLetterQueueProviderConfigurerModule.class);

    private final QuizupAxonDlqProperties properties;
    private final List<String> instanceScopedGroups;
    private final Function<String, Function<Configuration, SequencedDeadLetterQueue<EventMessage<?>>>> provider;

    public QuizupDeadLetterQueueProviderConfigurerModule(
            QuizupAxonDlqProperties properties,
            List<String> instanceScopedGroups,
            EventProcessorProperties axonProperties,
            Function<String, Function<Configuration, SequencedDeadLetterQueue<EventMessage<?>>>> provider) {
        super(axonProperties, provider);
        this.properties = properties;
        this.instanceScopedGroups = instanceScopedGroups;
        this.provider = provider;
    }

    @Override
    public void configureModule(Configurer configurer) {
        configurer.eventProcessing().registerDeadLetterQueueProvider(
                processingGroup -> isDlqEnabled(processingGroup, properties, instanceScopedGroups)
                        ? provider.apply(processingGroup)
                        : null);
        logger.info("QuizUp DLQ provider registered (excluded groups: {})", properties.excludedGroups());
    }

    /**
     * Règle d'activation de la DLQ pour un processing group.
     *
     * <ul>
     *     <li>DLQ globale désactivée → non ;</li>
     *     <li>groupe dans {@code excluded-groups} → non ;</li>
     *     <li>nom contenant un point (FQCN de saga) → non ;</li>
     *     <li>groupe instance-scopé (ou suffixé par un id d'instance) → non ;</li>
     *     <li>sinon → oui.</li>
     * </ul>
     */
    public static boolean isDlqEnabled(String processingGroup,
                                       QuizupAxonDlqProperties properties,
                                       List<String> instanceScopedGroups) {
        if (!Boolean.TRUE.equals(properties.enabled())) {
            return false;
        }
        if (processingGroup == null || processingGroup.isBlank()) {
            return false;
        }
        if (properties.excludedGroups().contains(processingGroup)) {
            return false;
        }
        if (processingGroup.indexOf('.') >= 0) {
            return false;
        }
        for (String baseGroup : instanceScopedGroups) {
            if (processingGroup.equals(baseGroup) || processingGroup.startsWith(baseGroup + "-")) {
                return false;
            }
        }
        return true;
    }
}
