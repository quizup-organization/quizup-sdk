package io.github.quizup.axon.console;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventhandling.deadletter.jpa.JpaDeadLetter;
import org.axonframework.messaging.StreamableMessageSource;
import org.axonframework.messaging.deadletter.Cause;
import org.axonframework.messaging.deadletter.DeadLetter;
import org.axonframework.messaging.deadletter.SequencedDeadLetterProcessor;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * API interne d'exploitation Axon, consommée par {@code quizup-console} (M2M, jamais ingressed) :
 * lecture des processors, gestion DLQ (retry / delete / clear), cycle de vie (pause / start /
 * reset / split / merge).
 *
 * <p>Le <b>reset</b> exige un processor à l'arrêt (contrainte Axon) : la console orchestre
 * pause → reset → start sur <b>toutes</b> les instances du processor.</p>
 */
@RestController
@RequestMapping("/internal/console")
public class ConsoleController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_ENTRIES_PER_SEQUENCE = 100;
    private static final int MAX_CAUSE_LENGTH = 500;
    private static final long ACTION_TIMEOUT_SECONDS = 30;

    private final ConsoleState state;

    public ConsoleController(ConsoleState state) {
        this.state = state;
    }

    // ------------------------------------------------------------------ processors

    @GetMapping("/processors")
    public List<ConsoleSnapshot.ProcessorSnapshot> processors() {
        return state.snapshot().processors();
    }

    @GetMapping("/processors/{processor}")
    public ConsoleSnapshot.ProcessorSnapshot processor(@PathVariable String processor) {
        return state.processor(processor);
    }

    @PostMapping("/processors/{processor}/pause")
    public void pause(@PathVariable String processor) {
        await(streamingProcessor(processor).shutdownAsync(), "pause", processor);
    }

    @PostMapping("/processors/{processor}/start")
    public void start(@PathVariable String processor) {
        streamingProcessor(processor).start();
    }

    @PostMapping("/processors/{processor}/reset")
    public ResetResponse reset(@PathVariable String processor, @RequestBody ResetRequest request) {
        StreamingEventProcessor streaming = streamingProcessor(processor);
        if (streaming.isRunning()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Processor must be paused before a reset: " + processor);
        }
        if (!streaming.supportsReset()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Processor does not support reset: " + processor);
        }
        String position = request.position() == null ? "TAIL" : request.position().toUpperCase(Locale.ROOT);
        Function<StreamableMessageSource<TrackedEventMessage<?>>, TrackingToken> tokenSupplier = switch (position) {
            case "TAIL" -> StreamableMessageSource::createTailToken;
            case "HEAD" -> StreamableMessageSource::createHeadToken;
            case "INSTANT" -> {
                if (request.instant() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "instant is required when position=INSTANT");
                }
                yield source -> source.createTokenAt(request.instant());
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown position (expected TAIL, HEAD or INSTANT): " + request.position());
        };
        streaming.resetTokens(tokenSupplier);
        return new ResetResponse(true, position, request.instant());
    }

    @PostMapping("/processors/{processor}/segments/{segmentId}/split")
    public SegmentActionResponse split(@PathVariable String processor, @PathVariable int segmentId) {
        boolean executed = await(streamingProcessor(processor).splitSegment(segmentId), "split", processor);
        return new SegmentActionResponse(executed);
    }

    @PostMapping("/processors/{processor}/segments/{segmentId}/merge")
    public SegmentActionResponse merge(@PathVariable String processor, @PathVariable int segmentId) {
        boolean executed = await(streamingProcessor(processor).mergeSegment(segmentId), "merge", processor);
        return new SegmentActionResponse(executed);
    }

    // ------------------------------------------------------------------ dead-letter queue

    @GetMapping("/dlq/{group}")
    public DlqPage dlq(@PathVariable String group,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "size", defaultValue = "20") int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int safePage = Math.max(0, page);
        SequencedDeadLetterQueue<EventMessage<?>> queue = deadLetterQueue(group);
        List<DlqSequence> sequences = new ArrayList<>();
        long skip = (long) safePage * safeSize;
        long index = 0;
        for (Iterable<DeadLetter<? extends EventMessage<?>>> sequence : queue.deadLetters()) {
            if (index++ < skip) {
                continue;
            }
            if (sequences.size() >= safeSize) {
                break;
            }
            sequences.add(toSequence(sequence));
        }
        return new DlqPage(safePage, safeSize, queue.amountOfSequences(), queue.size(), sequences);
    }

    @PostMapping("/dlq/{group}/sequences/{sequenceId}/retry")
    public RetryResponse retry(@PathVariable String group, @PathVariable String sequenceId) {
        deadLetterQueue(group);
        SequencedDeadLetterProcessor<EventMessage<?>> processor = state.eventProcessing()
                .sequencedDeadLetterProcessor(group)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No dead-letter processor for group: " + group));
        boolean processed = processor.process(letter -> sequenceKey(letter).equals(sequenceId));
        return new RetryResponse(processed);
    }

    @DeleteMapping("/dlq/{group}/sequences/{sequenceId}")
    public DeleteResponse delete(@PathVariable String group, @PathVariable String sequenceId) {
        SequencedDeadLetterQueue<EventMessage<?>> queue = deadLetterQueue(group);
        List<DeadLetter<? extends EventMessage<?>>> toEvict = null;
        for (Iterable<DeadLetter<? extends EventMessage<?>>> sequence : queue.deadLetters()) {
            List<DeadLetter<? extends EventMessage<?>>> letters = new ArrayList<>();
            sequence.forEach(letters::add);
            if (!letters.isEmpty() && sequenceKey(letters.get(0)).equals(sequenceId)) {
                toEvict = letters;
                break;
            }
        }
        if (toEvict == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Unknown dead-letter sequence: " + sequenceId);
        }
        toEvict.forEach(queue::evict);
        return new DeleteResponse(toEvict.size());
    }

    @PostMapping("/dlq/{group}/clear")
    public ClearResponse clear(@PathVariable String group) {
        deadLetterQueue(group).clear();
        return new ClearResponse(true);
    }

    // ------------------------------------------------------------------ helpers

    private StreamingEventProcessor streamingProcessor(String name) {
        return state.eventProcessing().eventProcessor(name, StreamingEventProcessor.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Unknown streaming processor: " + name));
    }

    private SequencedDeadLetterQueue<EventMessage<?>> deadLetterQueue(String group) {
        return state.deadLetterQueue(group)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No dead-letter queue for processing group: " + group));
    }

    private DlqSequence toSequence(Iterable<DeadLetter<? extends EventMessage<?>>> sequence) {
        List<DlqEntry> entries = new ArrayList<>();
        String sequenceId = null;
        long total = 0;
        boolean truncated = false;
        for (DeadLetter<? extends EventMessage<?>> letter : sequence) {
            total++;
            if (sequenceId == null) {
                sequenceId = sequenceKey(letter);
            }
            if (entries.size() >= MAX_ENTRIES_PER_SEQUENCE) {
                truncated = true;
                continue;
            }
            entries.add(toEntry(letter));
        }
        return new DlqSequence(sequenceId, total, truncated, entries);
    }

    private DlqEntry toEntry(DeadLetter<? extends EventMessage<?>> letter) {
        EventMessage<?> message = letter.message();
        Cause cause = letter.cause().orElse(null);
        return new DlqEntry(
                message.getIdentifier(),
                letter instanceof JpaDeadLetter<?> jpa ? jpa.getIndex() : null,
                message.getPayloadType() != null ? message.getPayloadType().getName() : null,
                cause != null ? cause.type() : null,
                cause != null ? truncate(cause.message()) : null,
                letter.enqueuedAt(),
                letter.lastTouched());
    }

    private static String sequenceKey(DeadLetter<? extends EventMessage<?>> letter) {
        return letter instanceof JpaDeadLetter<?> jpa
                ? jpa.getSequenceIdentifier()
                : letter.message().getIdentifier();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_CAUSE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_CAUSE_LENGTH) + "…";
    }

    private static <T> T await(CompletableFuture<T> future, String action, String processor) {
        try {
            return future.get(ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                    "Timed out while trying to " + action + " processor: " + processor);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Interrupted while trying to " + action + " processor: " + processor);
        } catch (ExecutionException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not " + action + " processor " + processor + ": " + exception.getCause());
        }
    }

    // ------------------------------------------------------------------ contracts

    public record ResetRequest(String position, Instant instant) {
    }

    public record ResetResponse(boolean reset, String position, Instant instant) {
    }

    public record SegmentActionResponse(boolean executed) {
    }

    public record DlqPage(int page, int size, long totalSequences, long totalSize, List<DlqSequence> sequences) {
    }

    public record DlqSequence(String sequenceId, long size, boolean truncated, List<DlqEntry> entries) {
    }

    public record DlqEntry(String id, Long index, String payloadType, String causeType, String causeMessage,
                           Instant enqueuedAt, Instant lastTouched) {
    }

    public record RetryResponse(boolean processed) {
    }

    public record DeleteResponse(int deleted) {
    }

    public record ClearResponse(boolean cleared) {
    }
}
