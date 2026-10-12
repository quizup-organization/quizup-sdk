package io.github.quizup.axon.console;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot d'exploitation poussé par l'agent SDK vers {@code quizup-console}
 * ({@code POST /internal/ingest/snapshot}) et servi par l'API interne.
 *
 * <p>Payload <b>borné</b> (aucun payload d'événement) et <b>versionné</b> ({@link #VERSION}) :
 * le service console doit accepter les anciennes versions d'agent.</p>
 */
public record ConsoleSnapshot(
        String version,
        Instant timestamp,
        String application,
        String applicationVersion,
        String instanceId,
        long uptimeSeconds,
        List<ProcessorSnapshot> processors
) {

    public static final String VERSION = "v1";

    public record ProcessorSnapshot(
            String name,
            String mode,
            boolean running,
            boolean error,
            boolean replaying,
            boolean supportsReset,
            List<SegmentSnapshot> segments,
            DlqStats dlq
    ) {
    }

    public record SegmentSnapshot(
            int segmentId,
            int mask,
            boolean caughtUp,
            boolean replaying,
            boolean merging,
            boolean errorState,
            Long currentPosition,
            Long resetPosition,
            String errorType,
            String errorMessage
    ) {
    }

    public record DlqStats(long size, long sequences) {
    }
}
