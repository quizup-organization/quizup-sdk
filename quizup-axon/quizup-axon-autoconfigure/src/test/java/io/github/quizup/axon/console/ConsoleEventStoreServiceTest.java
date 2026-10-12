package io.github.quizup.axon.console;

import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsoleEventStoreServiceTest {

    @Test
    void parsesEpochMillisAndIsoTimestamps() {
        assertThat(ConsoleEventStoreService.parseTimestamp("1791792493159"))
                .isEqualTo(Instant.ofEpochMilli(1791792493159L));
        assertThat(ConsoleEventStoreService.parseTimestamp("2026-10-12T08:09:25.280Z"))
                .isEqualTo(Instant.parse("2026-10-12T08:09:25.280Z"));
        assertThat(ConsoleEventStoreService.parseTimestamp("not-a-date")).isNull();
        assertThat(ConsoleEventStoreService.parseTimestamp(null)).isNull();
    }

    @Test
    void mapsAggregateEventsFromTheEventStore() {
        EventStore eventStore = mock(EventStore.class);
        GenericDomainEventMessage<String> event = new GenericDomainEventMessage<>(
                "DemoAggregate", "demo-1", 2L, "payload", Map.of("traceId", "t-1"),
                "event-1", Instant.parse("2026-10-12T08:09:25.280Z"));
        when(eventStore.readEvents("demo-1")).thenReturn(DomainEventStream.of(event));

        ConsoleEventStoreService service = new ConsoleEventStoreService(provider(null), provider(eventStore), provider(null), provider(null));

        var entries = service.aggregateEvents("demo-1", null);

        assertThat(entries).hasSize(1);
        var entry = entries.getFirst();
        assertThat(entry.id()).isEqualTo("event-1");
        assertThat(entry.aggregateId()).isEqualTo("demo-1");
        assertThat(entry.aggregateType()).isEqualTo("DemoAggregate");
        assertThat(entry.sequenceNumber()).isEqualTo(2L);
        assertThat(entry.payload()).isEqualTo("payload");
        assertThat(entry.metaData()).containsEntry("traceId", "t-1");
        assertThat(entry.timestamp()).isEqualTo(Instant.parse("2026-10-12T08:09:25.280Z"));
    }

    @Test
    void filtersAggregateEventsBelowMinSequence() {
        EventStore eventStore = mock(EventStore.class);
        when(eventStore.readEvents("demo-1")).thenReturn(DomainEventStream.of(
                new GenericDomainEventMessage<>("DemoAggregate", "demo-1", 0L, "a"),
                new GenericDomainEventMessage<>("DemoAggregate", "demo-1", 1L, "b")));

        ConsoleEventStoreService service = new ConsoleEventStoreService(provider(null), provider(eventStore), provider(null), provider(null));

        assertThat(service.aggregateEvents("demo-1", 1L)).hasSize(1);
    }

    @Test
    void statsFailWithServiceUnavailableWithoutDataSource() {
        ConsoleEventStoreService service =
                new ConsoleEventStoreService(provider(null), provider(null), provider(null), provider(null));

        assertThatThrownBy(service::stats)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("DataSource");
    }

    @Test
    void cancelScheduledEventFailsWithServiceUnavailableWithoutScheduler() {
        ConsoleEventStoreService service =
                new ConsoleEventStoreService(provider(null), provider(null), provider(null), provider(null));

        assertThatThrownBy(() -> service.cancelScheduledEvent("task", "instance"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Scheduler");
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }
        };
    }
}
