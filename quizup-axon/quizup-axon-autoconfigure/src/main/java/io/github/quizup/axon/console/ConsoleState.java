package io.github.quizup.axon.console;

import org.axonframework.config.Configuration;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventhandling.EventTrackerStatus;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.axonframework.eventhandling.SubscribingEventProcessor;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lecture de l'état d'exploitation local (event processors, segments, DLQ) sous forme de
 * {@link ConsoleSnapshot}. Utilisé par l'agent (push) et par l'API interne (lecture à la demande).
 *
 * <p>{@link Configuration} est résolu <b>à l'usage</b> (jamais à la construction) : toucher les
 * processors pendant l'initialisation Axon casse le démarrage (cf. pièges connus).</p>
 */
public class ConsoleState {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleState.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final ObjectProvider<Configuration> configurationProvider;
    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final QuizupAxonConsoleProperties properties;

    public ConsoleState(ObjectProvider<Configuration> configurationProvider,
                        Environment environment,
                        ObjectProvider<BuildProperties> buildProperties,
                        QuizupAxonConsoleProperties properties) {
        this.configurationProvider = configurationProvider;
        this.environment = environment;
        this.buildProperties = buildProperties;
        this.properties = properties;
    }

    public EventProcessingConfiguration eventProcessing() {
        Configuration configuration = configurationProvider.getIfAvailable();
        if (configuration == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Axon Configuration is not available");
        }
        return configuration.eventProcessingConfiguration();
    }

    public ConsoleSnapshot snapshot() {
        EventProcessingConfiguration eventProcessing = eventProcessing();
        List<ConsoleSnapshot.ProcessorSnapshot> processors = eventProcessing.eventProcessors()
                .entrySet()
                .stream()
                .map(entry -> processorSnapshot(entry.getKey(), entry.getValue()))
                .toList();
        return new ConsoleSnapshot(
                ConsoleSnapshot.VERSION,
                java.time.Instant.now(),
                environment.getProperty("spring.application.name", "unknown"),
                applicationVersion(),
                instanceId(),
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000,
                processors);
    }

    public ConsoleSnapshot.ProcessorSnapshot processor(String name) {
        EventProcessingConfiguration eventProcessing = eventProcessing();
        EventProcessor processor = eventProcessing.eventProcessors().get(name);
        if (processor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown processor: " + name);
        }
        return processorSnapshot(name, processor);
    }

    /**
     * DLQ d'un processing group, ou {@link Optional#empty()} s'il n'y en a pas. Les groupes de
     * <b>sagas</b> sont exclus : Axon ne les wrappe pas avec la DLQ (pas de dead-lettering possible).
     */
    public Optional<SequencedDeadLetterQueue<EventMessage<?>>> deadLetterQueue(String processingGroup) {
        EventProcessingConfiguration eventProcessing = eventProcessing();
        if (sagaGroups(eventProcessing).contains(processingGroup)) {
            return Optional.empty();
        }
        return eventProcessing.deadLetterQueue(processingGroup);
    }

    public static Set<String> sagaGroups(EventProcessingConfiguration eventProcessing) {
        return eventProcessing.sagaConfigurations()
                .stream()
                .map(SagaConfiguration::processingGroup)
                .collect(Collectors.toSet());
    }

    private ConsoleSnapshot.ProcessorSnapshot processorSnapshot(String name, EventProcessor processor) {
        boolean running = processor.isRunning();
        boolean error = processor.isError();
        if (processor instanceof StreamingEventProcessor streaming) {
            Map<Integer, EventTrackerStatus> statuses = streaming.processingStatus();
            int segmentCount = statuses.size();
            int capacity = streaming.maxCapacity();
            List<ConsoleSnapshot.SegmentSnapshot> segments = statuses.entrySet()
                    .stream()
                    .map(entry -> segmentSnapshot(entry.getKey(), entry.getValue(), segmentCount))
                    .toList();
            return new ConsoleSnapshot.ProcessorSnapshot(
                    name, "tracking", running, error, streaming.isReplaying(),
                    streaming.supportsReset(),
                    running, !running, segmentCount < capacity, segmentCount > 1,
                    streaming.getTokenStoreIdentifier(), segmentCount, capacity,
                    List.of(), warnings(segments), segments, dlqStats(name));
        }
        String mode = processor instanceof SubscribingEventProcessor ? "subscribing" : processor.getClass().getSimpleName();
        return new ConsoleSnapshot.ProcessorSnapshot(
                name, mode, running, error, false, false,
                running, !running, false, false, null, 0, 0,
                List.of(), List.of(), List.of(), dlqStats(name));
    }

    private static List<String> warnings(List<ConsoleSnapshot.SegmentSnapshot> segments) {
        List<String> warnings = new ArrayList<>();
        long errors = segments.stream().filter(ConsoleSnapshot.SegmentSnapshot::errorState).count();
        if (errors > 0) {
            warnings.add(errors + " segment(s) en erreur");
        }
        long replaying = segments.stream().filter(ConsoleSnapshot.SegmentSnapshot::replaying).count();
        if (replaying > 0) {
            warnings.add("Replay en cours sur " + replaying + " segment(s)");
        }
        long merging = segments.stream().filter(ConsoleSnapshot.SegmentSnapshot::merging).count();
        if (merging > 0) {
            warnings.add("Fusion de segments en cours");
        }
        return List.copyOf(warnings);
    }

    private ConsoleSnapshot.SegmentSnapshot segmentSnapshot(int segmentId, EventTrackerStatus status, int onePartOf) {
        Throwable segmentError = status.isErrorState() ? status.getError() : null;
        return new ConsoleSnapshot.SegmentSnapshot(
                segmentId,
                status.getSegment().getMask(),
                status.isCaughtUp(),
                status.isReplaying(),
                status.isMerging(),
                status.isErrorState(),
                optionalPosition(status.getCurrentPosition()),
                optionalPosition(status.getResetPosition()),
                onePartOf,
                segmentError != null ? segmentError.getClass().getName() : null,
                Boolean.TRUE.equals(properties.includeErrorDetails()) ? truncate(segmentError) : null);
    }

    private ConsoleSnapshot.DlqStats dlqStats(String name) {
        try {
            return deadLetterQueue(name)
                    .map(queue -> new ConsoleSnapshot.DlqStats(queue.size(), queue.amountOfSequences()))
                    .orElse(null);
        } catch (RuntimeException exception) {
            logger.warn("Could not read DLQ stats for processor '{}': {}", name, exception.toString());
            return null;
        }
    }

    private static Long optionalPosition(java.util.OptionalLong position) {
        return position.isPresent() ? position.getAsLong() : null;
    }

    private static String truncate(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return null;
        }
        String message = throwable.getMessage();
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH) + "…";
    }

    private String applicationVersion() {
        BuildProperties build = buildProperties.getIfAvailable();
        if (build != null && build.getVersion() != null) {
            return build.getVersion();
        }
        return environment.getProperty("info.app.version", "unknown");
    }

    private String instanceId() {
        String configured = properties.instanceId();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return environment.getProperty("HOSTNAME", "local");
    }

    /**
     * Exposition des statuts de segments pour les jauges (aucun I/O hormis la DLQ).
     */
    public Map<String, EventProcessor> processors() {
        return eventProcessing().eventProcessors();
    }

    public static boolean caughtUp(EventProcessor processor) {
        if (processor instanceof StreamingEventProcessor streaming) {
            return streaming.processingStatus().values().stream().allMatch(EventTrackerStatus::isCaughtUp);
        }
        return true;
    }

    public static boolean replaying(EventProcessor processor) {
        return processor instanceof StreamingEventProcessor streaming && streaming.isReplaying();
    }
}
