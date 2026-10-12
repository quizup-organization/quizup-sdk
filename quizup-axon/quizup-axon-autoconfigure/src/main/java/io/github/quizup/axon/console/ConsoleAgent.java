package io.github.quizup.axon.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent de push de l'état Axon vers {@code quizup-console} (équivalent open source de l'agent
 * Axoniq Platform) : snapshot périodique (défaut 15 s) des event processors, segments et DLQ,
 * envoyé en M2M via le {@link RestTemplate} OAuth2 {@code @Primary} du SDK.
 *
 * <p>Activé par {@code quizup.axon.console.enabled=true} (défaut {@code false} tant que le service
 * console n'est pas déployé). Un échec de push ne doit <b>jamais</b> impacter le service : toute
 * exception est journalisée en WARN.</p>
 */
public class ConsoleAgent implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleAgent.class);
    private static final long INITIAL_DELAY_SECONDS = 10;

    private final ConsoleState state;
    private final ObjectProvider<RestTemplate> restTemplateProvider;
    private final QuizupAxonConsoleProperties properties;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "axon-console-agent");
        thread.setDaemon(true);
        return thread;
    });

    public ConsoleAgent(ConsoleState state,
                        ObjectProvider<RestTemplate> restTemplateProvider,
                        QuizupAxonConsoleProperties properties) {
        this.state = state;
        this.restTemplateProvider = restTemplateProvider;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!Boolean.TRUE.equals(properties.enabled()) || !started.compareAndSet(false, true)) {
            return;
        }
        long intervalMillis = Math.max(1000L, properties.interval().toMillis());
        long intervalSeconds = Math.max(1L, intervalMillis / 1000L);
        scheduler.scheduleWithFixedDelay(this::safePush, INITIAL_DELAY_SECONDS, intervalSeconds, TimeUnit.SECONDS);
        logger.info("Axon console agent started (url={}, interval={})", properties.url(), properties.interval());
    }

    @Override
    public void destroy() {
        started.set(false);
        scheduler.shutdownNow();
    }

    void safePush() {
        if (!started.get()) {
            return;
        }
        try {
            push(state.snapshot());
        } catch (RuntimeException exception) {
            logger.warn("Could not push console snapshot to {}: {}", properties.url(), exception.toString());
        }
    }

    private void push(ConsoleSnapshot snapshot) {
        RestTemplate restTemplate = restTemplateProvider.getIfAvailable();
        if (restTemplate == null) {
            throw new IllegalStateException("No RestTemplate bean available for the console agent");
        }
        restTemplate.postForEntity(properties.url() + "/internal/ingest/snapshot", snapshot, Void.class);
    }
}
