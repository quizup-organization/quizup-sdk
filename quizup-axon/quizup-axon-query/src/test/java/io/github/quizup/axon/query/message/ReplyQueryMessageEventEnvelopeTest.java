package io.github.quizup.axon.query.message;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.quizup.microservice.core.domain.model.notification.EventEnvelope;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.GenericQueryResponseMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.axonframework.serialization.SerializationException;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verrouille le codec transport de l'{@link EventEnvelope} : le payload d'une liste d'enveloppes
 * conserve son type concret après le round-trip {@code ReplyQueryMessage} (avant correctif, le
 * payload générique était effacé en {@code Map}).
 */
class ReplyQueryMessageEventEnvelopeTest {

    record SampleEvent(String gameId, int score) {
    }

    private final Serializer serializer = JacksonSerializer.builder()
            .objectMapper(JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .addModule(new EventEnvelopeModule())
                    .build())
            .build();

    @Test
    void payloadTypeSurvivesWire() {
        Instant occurredAt = Instant.parse("2026-09-27T10:00:00Z");
        EventEnvelope envelope = EventEnvelope.of("game-1", 3, occurredAt, new SampleEvent("game-1", 120));

        QueryResponseMessage<?> back = exchange(List.of(envelope));

        List<?> envelopes = assertInstanceOf(List.class, back.getPayload());
        EventEnvelope restored = assertInstanceOf(EventEnvelope.class, envelopes.getFirst());
        assertEquals("game-1", restored.aggregateId());
        assertEquals(3, restored.sequenceNumber());
        assertEquals(occurredAt, restored.timestamp());
        assertEquals(SampleEvent.class.getName(), restored.eventType());
        assertEquals(new SampleEvent("game-1", 120), restored.payload());
    }

    @Test
    void nullPayloadSurvivesWire() {
        EventEnvelope envelope = EventEnvelope.of("game-1", 1, Instant.now(), null);

        QueryResponseMessage<?> back = exchange(List.of(envelope));

        List<?> envelopes = assertInstanceOf(List.class, back.getPayload());
        EventEnvelope restored = assertInstanceOf(EventEnvelope.class, envelopes.getFirst());
        assertNull(restored.eventType());
        assertNull(restored.payload());
    }

    @Test
    void nonAllowlistedPayloadTypeIsRejected() {
        EventEnvelope envelope = EventEnvelope.of("game-1", 1, Instant.now(), "not-a-quizup-type");

        ReplyQueryMessage wire = new ReplyQueryMessage(
                "q",
                new GenericQueryResponseMessage<>(List.of(envelope)),
                serializer,
                responseType());

        assertThrows(SerializationException.class, () -> wire.getQueryResponseMessage(serializer));
    }

    private QueryResponseMessage<?> exchange(List<EventEnvelope> envelopes) {
        ReplyQueryMessage wire = new ReplyQueryMessage(
                "q",
                new GenericQueryResponseMessage<>(envelopes),
                serializer,
                responseType());
        return wire.getQueryResponseMessage(serializer);
    }

    @SuppressWarnings("unchecked")
    private static ResponseType<List<EventEnvelope>> responseType() {
        return (ResponseType<List<EventEnvelope>>) (ResponseType<?>)
                QueryResponseTypes.multipleInstancesOf(EventEnvelope.class);
    }
}
