package io.github.quizup.axon.autoconfigure;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;
import io.github.quizup.axon.deadline.CombinedDeadlineManager;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.ConfigurationScopeAwareProvider;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.DeadlineManagerSpanFactory;
import org.axonframework.deadline.SimpleDeadlineManager;
import org.axonframework.deadline.dbscheduler.DbSchedulerBinaryDeadlineDetails;
import org.axonframework.deadline.dbscheduler.DbSchedulerDeadlineManager;
import org.axonframework.serialization.Serializer;
import org.axonframework.springboot.autoconfig.AxonAutoConfiguration;
import org.axonframework.springboot.autoconfig.AxonDbSchedulerAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * Reprend la main sur le {@link DeadlineManager} afin de fournir un {@link CombinedDeadlineManager}
 * quand db-scheduler est disponible : deadlines courtes (&lt; 5 min) en mémoire (transient), deadlines
 * longues persistées/distribuées (db-scheduler). Sinon, repli sur un {@link SimpleDeadlineManager}.
 *
 * <p>Axon expose par défaut un {@code DeadlineManager} db-scheduler de type déclaré
 * {@code DeadlineManager} (donc non injectable en tant que {@code DbSchedulerDeadlineManager}). Pour
 * éviter deux candidats, cette auto-configuration est traitée <b>avant</b>
 * {@link AxonDbSchedulerAutoConfiguration} : son bean {@code deadlineManager} (et sa tâche
 * {@code deadlineDetailsTask}) sont ainsi supprimés au profit des nôtres, ce qui garantit
 * exactement <b>un seul</b> bean {@code DeadlineManager}.</p>
 */
@AutoConfiguration(
        before = {
                AxonAutoConfiguration.class,
                AxonDbSchedulerAutoConfiguration.class
        },
        afterName = "com.github.kagkarlsson.scheduler.boot.autoconfigure.DbSchedulerAutoConfiguration"
)
public class AxonDistributedDeadlineAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AxonDistributedDeadlineAutoConfiguration.class);

    @Bean
    public DeadlineManager deadlineManager(
            org.axonframework.config.Configuration configuration,
            TransactionManager transactionManager,
            ObjectProvider<PersistentDeadlineManagerHolder> persistentHolderProvider
    ) {
        DeadlineManager transientDeadlineManager = simpleDeadlineManager(configuration, transactionManager);

        PersistentDeadlineManagerHolder persistentHolder = persistentHolderProvider.getIfAvailable();
        if (persistentHolder == null) {
            logger.info("Deadline manager: SimpleDeadlineManager (in-memory) — db-scheduler unavailable");
            return transientDeadlineManager;
        }

        logger.info("Deadline manager: CombinedDeadlineManager (transient in-memory + db-scheduler persistent)");
        return new CombinedDeadlineManager(transientDeadlineManager, persistentHolder.manager());
    }

    /**
     * Construit le manager persistant db-scheduler, détenu hors bean {@code DeadlineManager} pour
     * ne pas créer de candidat supplémentaire. Exposé via un holder consommé par le manager combiné
     * et par la tâche db-scheduler.
     */
    @Bean
    @ConditionalOnBean(Scheduler.class)
    public PersistentDeadlineManagerHolder persistentDeadlineManagerHolder(
            Scheduler scheduler,
            org.axonframework.config.Configuration configuration,
            @Qualifier("eventSerializer") Serializer serializer,
            TransactionManager transactionManager,
            ObjectProvider<DeadlineManagerSpanFactory> spanFactoryProvider
    ) {
        DbSchedulerDeadlineManager.Builder builder = DbSchedulerDeadlineManager.builder()
                .scheduler(scheduler)
                .scopeAwareProvider(new ConfigurationScopeAwareProvider(configuration))
                .serializer(serializer)
                .transactionManager(transactionManager)
                // Le DbSchedulerStarter du starter db-scheduler gère le cycle de vie.
                .startScheduler(false)
                .stopScheduler(false);

        DeadlineManagerSpanFactory spanFactory = spanFactoryProvider.getIfAvailable();
        if (spanFactory != null) {
            builder.spanFactory(spanFactory);
        }

        return new PersistentDeadlineManagerHolder(builder.build());
    }

    /**
     * Tâche db-scheduler des deadlines. Supprime celle d'{@code AxonDbSchedulerAutoConfiguration}
     * (même qualifieur) et pointe vers notre manager persistant.
     *
     * <p>Le manager est résolu <b>paresseusement</b> ({@link ObjectProvider#getObject()}) : le
     * {@code Scheduler} de db-scheduler est construit à partir des beans {@link Task}, donc une
     * dépendance directe créerait un cycle
     * {@code Scheduler → task → holder → Scheduler}.</p>
     */
    @Bean
    @ConditionalOnBean(Scheduler.class)
    @Qualifier("deadlineDetailsTask")
    public Task<DbSchedulerBinaryDeadlineDetails> dbSchedulerDeadlineDetailsTask(
            ObjectProvider<PersistentDeadlineManagerHolder> holderProvider
    ) {
        return DbSchedulerDeadlineManager.binaryTask(() -> holderProvider.getObject().manager());
    }

    private DeadlineManager simpleDeadlineManager(
            org.axonframework.config.Configuration configuration,
            TransactionManager transactionManager
    ) {
        return new SimpleDeadlineManager.Builder()
                .scopeAwareProvider(new ConfigurationScopeAwareProvider(configuration))
                .transactionManager(transactionManager)
                .build();
    }

    /**
     * Détient le {@link DbSchedulerDeadlineManager} sans être un bean {@link DeadlineManager},
     * afin de ne pas perturber la résolution du bean unique {@code DeadlineManager}.
     */
    public record PersistentDeadlineManagerHolder(DbSchedulerDeadlineManager manager) {
    }
}
