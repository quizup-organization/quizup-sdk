package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.console.CommandActivityTracker;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.common.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Enregistre le {@link CommandActivityTracker} (commandes en vol) sur le bus de commandes
 * distribué, selon le même modèle que {@link AxonMetricsInterceptorConfiguration} : résolution
 * paresseuse du bus + {@code @PostConstruct}, jamais un {@code ConfigurerModule}/{@code onInitialize}.
 *
 * <p><b>Piège</b> : résoudre le {@code CommandBus} pendant l'initialisation de la
 * {@code Configuration} Axon ({@code onInitialize}) crée un cycle de beans
 * ({@code springAxonConfiguration} → bus distribué → {@code Configuration}) dès qu'une projection
 * injecte {@code CommandGateway} (theme, game, social, notification) — le bus distribué dépend de
 * la {@code Configuration} en cours de création. Ici la résolution est différée au
 * {@code @PostConstruct} : la {@code Configuration} existe déjà (ou est créée à la demande sans
 * cycle).</p>
 */
public class CommandActivityTrackerRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(CommandActivityTrackerRegistrar.class);

    private final CommandActivityTracker tracker;
    private final ObjectProvider<CommandBus> distributedCommandBus;
    private final ObjectProvider<CommandBus> anyCommandBus;
    private Registration registration;

    public CommandActivityTrackerRegistrar(
            CommandActivityTracker tracker,
            @Qualifier("distributedCommandBus") ObjectProvider<CommandBus> distributedCommandBus,
            ObjectProvider<CommandBus> anyCommandBus) {
        this.tracker = tracker;
        this.distributedCommandBus = distributedCommandBus;
        this.anyCommandBus = anyCommandBus;
    }

    @PostConstruct
    public void register() {
        CommandBus bus = distributedCommandBus.getIfAvailable();
        if (bus == null) {
            bus = anyCommandBus.getIfUnique();
        }
        if (bus == null) {
            logger.debug("Aucun CommandBus disponible — suivi des commandes en vol désactivé");
            return;
        }
        registration = bus.registerHandlerInterceptor(tracker);
        logger.info("Suivi des commandes en vol enregistré sur le bus de commandes");
    }

    @PreDestroy
    public void unregister() {
        if (registration != null) {
            registration.close();
        }
    }
}
