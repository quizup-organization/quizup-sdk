package io.github.quizup.axon.console;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture de l'<b>event store JPA</b> local pour l'API interne d'exploitation (équivalent du
 * contenu natif Axon Server : stats, événements par agrégat, snapshots, recherche par type) et des
 * <b>deadlines persistantes</b> ({@code scheduled_tasks} db-scheduler).
 *
 * <p>Les événements par agrégat passent par l'API publique {@link EventStore#readEvents(String)}
 * (payload désérialisé par Axon, depuis le dernier snapshot) ; les comptages, snapshots, recherche
 * par type et tâches planifiées passent par des requêtes SQL <b>read-only</b> sur le schéma Axon
 * standard (aucune écriture).</p>
 */
public class ConsoleEventStoreService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleEventStoreService.class);
    private static final int MAX_LIMIT = 200;
    private static final int MAX_SNAPSHOTS = 100;

    private final ObjectProvider<DataSource> dataSourceProvider;
    private final ObjectProvider<EventStore> eventStoreProvider;
    private final ObjectProvider<Scheduler> schedulerProvider;
    private final ObjectProvider<Serializer> eventSerializerProvider;

    public ConsoleEventStoreService(ObjectProvider<DataSource> dataSourceProvider,
                                    ObjectProvider<EventStore> eventStoreProvider,
                                    ObjectProvider<Scheduler> schedulerProvider,
                                    ObjectProvider<Serializer> eventSerializerProvider) {
        this.dataSourceProvider = dataSourceProvider;
        this.eventStoreProvider = eventStoreProvider;
        this.schedulerProvider = schedulerProvider;
        this.eventSerializerProvider = eventSerializerProvider;
    }

    public EventStoreStats stats() {
        return jdbc().queryForObject(
                "select (select count(*) from domain_event_entry), (select count(*) from snapshot_event_entry)",
                (rs, rowNum) -> new EventStoreStats(rs.getLong(1), rs.getLong(2)));
    }

    public List<AggregateEventEntry> aggregateEvents(String aggregateId, Long minSequence) {
        EventStore eventStore = eventStore();
        DomainEventStream stream = eventStore.readEvents(aggregateId);
        List<AggregateEventEntry> entries = new ArrayList<>();
        while (stream.hasNext()) {
            DomainEventMessage<?> event = stream.next();
            if (minSequence != null && event.getSequenceNumber() < minSequence) {
                continue;
            }
            entries.add(toEntry(event));
        }
        return entries;
    }

    public List<SnapshotEntry> snapshots(String aggregateId, boolean withPayload) {
        String payloadColumn = withPayload ? ", lo_get(payload)" : "";
        return jdbc().query(
                "select event_identifier, aggregate_identifier, sequence_number, type, payload_type,"
                        + " payload_revision, time_stamp" + payloadColumn + " from snapshot_event_entry"
                        + " where aggregate_identifier = ? order by sequence_number desc limit " + MAX_SNAPSHOTS,
                (rs, rowNum) -> new SnapshotEntry(
                        rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), parseTimestamp(rs.getString(7)),
                        withPayload ? decodePayload(rs.getBytes(8), rs.getString(5), rs.getString(6)) : null),
                aggregateId);
    }

    private Object decodePayload(byte[] bytes, String payloadType, String revision) {
        Serializer serializer = eventSerializerProvider.getIfAvailable();
        if (serializer == null || bytes == null || payloadType == null) {
            return null;
        }
        try {
            SerializedObject<byte[]> serialized =
                    new SimpleSerializedObject<>(bytes, byte[].class, payloadType, revision);
            return serializer.deserialize(serialized);
        } catch (RuntimeException exception) {
            logger.warn("Could not deserialize snapshot payload of type '{}': {}",
                    payloadType, exception.toString());
            return null;
        }
    }

    /** Annule une tâche planifiée (deadline) — contenu natif Axon Server (« cancel scheduled event »). */
    public void cancelScheduledEvent(String taskName, String taskInstance) {
        Scheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Scheduler is not available");
        }
        try {
            scheduler.cancel(TaskInstanceId.of(taskName, taskInstance));
        } catch (TaskInstanceNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Unknown scheduled event: " + taskName + "/" + taskInstance);
        }
    }

    public List<EventSearchEntry> searchEvents(String type, Long from, Integer limit) {
        int size = limit == null ? 50 : Math.max(1, Math.min(limit, MAX_LIMIT));
        long fromIndex = from == null ? 0L : Math.max(0L, from);
        String like = "%" + (type == null ? "" : type.trim()) + "%";
        logger.debug("Searching events by type '{}' from index {} (limit {})", like, fromIndex, size);
        return jdbc().query(
                "select event_identifier, aggregate_identifier, type, sequence_number, payload_type,"
                        + " payload_revision, global_index, time_stamp from domain_event_entry"
                        + " where payload_type like ? and global_index >= ?"
                        + " order by global_index asc limit " + size,
                (rs, rowNum) -> new EventSearchEntry(
                        rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                        rs.getString(5), rs.getString(6), rs.getLong(7), parseTimestamp(rs.getString(8))),
                like, fromIndex);
    }

    public List<ScheduledEventEntry> scheduledEvents(Integer limit) {
        int size = limit == null ? 50 : Math.max(1, Math.min(limit, MAX_LIMIT));
        return jdbc().query(
                "select task_name, task_instance, execution_time, picked, last_success, last_failure,"
                        + " consecutive_failures from scheduled_tasks"
                        + " order by execution_time asc limit " + size,
                (rs, rowNum) -> new ScheduledEventEntry(
                        rs.getString(1), rs.getString(2), toInstant(rs.getTimestamp(3)),
                        rs.getBoolean(4), toInstant(rs.getTimestamp(5)), toInstant(rs.getTimestamp(6)),
                        (Integer) rs.getObject(7)));
    }

    private JdbcTemplate jdbc() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DataSource is not available");
        }
        return new JdbcTemplate(dataSource);
    }

    private EventStore eventStore() {
        EventStore eventStore = eventStoreProvider.getIfAvailable();
        if (eventStore == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "EventStore is not available");
        }
        return eventStore;
    }

    private static AggregateEventEntry toEntry(DomainEventMessage<?> event) {
        return new AggregateEventEntry(
                event.getIdentifier(),
                event.getAggregateIdentifier(),
                event.getType(),
                event.getSequenceNumber(),
                event.getPayloadType() != null ? event.getPayloadType().getName() : null,
                event.getPayload(),
                new LinkedHashMap<>(event.getMetaData()),
                event.getTimestamp());
    }

    static Instant parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return Instant.ofEpochMilli(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            // pas un epoch millis : format ISO attendu
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static Instant toInstant(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    // ------------------------------------------------------------------ contracts

    public record EventStoreStats(long events, long snapshots) {
    }

    public record AggregateEventEntry(String id, String aggregateId, String aggregateType,
                                      long sequenceNumber, String payloadType, Object payload,
                                      Map<String, Object> metaData, Instant timestamp) {
    }

    public record SnapshotEntry(String id, String aggregateId, long sequenceNumber, String aggregateType,
                                String payloadType, String payloadRevision, Instant timestamp, Object payload) {
    }

    public record EventSearchEntry(String id, String aggregateId, String aggregateType, long sequenceNumber,
                                   String payloadType, String payloadRevision, long globalIndex,
                                   Instant timestamp) {
    }

    public record ScheduledEventEntry(String taskName, String taskInstance, Instant executionTime,
                                      boolean picked, Instant lastSuccess, Instant lastFailure,
                                      Integer consecutiveFailures) {
    }
}
