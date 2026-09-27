package io.github.quizup.axon.query.message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.quizup.microservice.core.domain.model.notification.EventEnvelope;

import java.io.IOException;
import java.time.Instant;

/**
 * Codec Jackson de l'{@link EventEnvelope} pour le query bus distribué : le payload est
 * (dé)sérialisé avec son type concret à partir de {@code eventType}, ce qui rend inutile tout
 * typage polymorphe global ou annotation framework dans les modules domaine.
 *
 * <p>Sérialisation : {@code eventType} est dérivé du type runtime du payload (nom qualifié
 * complet). Désérialisation : {@code eventType} doit appartenir à l'allowlist
 * {@value #ALLOWED_PACKAGE_PREFIX} (fail-closed), puis la classe est chargée et le payload bindé.
 * Le codec n'est enregistré que dans le serializer dédié au query bus distribué (le format de
 * l'event store et le contrat web ne sont pas affectés).</p>
 */
public final class EventEnvelopeModule extends SimpleModule {

    static final String ALLOWED_PACKAGE_PREFIX = "io.github.quizup.";

    public EventEnvelopeModule() {
        super("quizup-event-envelope");
        addSerializer(EventEnvelope.class, new EventEnvelopeSerializer());
        addDeserializer(EventEnvelope.class, new EventEnvelopeDeserializer());
    }

    private static final class EventEnvelopeSerializer extends StdSerializer<EventEnvelope> {

        private EventEnvelopeSerializer() {
            super(EventEnvelope.class);
        }

        @Override
        public void serialize(EventEnvelope value,
                              JsonGenerator generator,
                              SerializerProvider provider) throws IOException {
            Object payload = value.payload();
            generator.writeStartObject();
            generator.writeStringField("aggregateId", value.aggregateId());
            generator.writeNumberField("sequenceNumber", value.sequenceNumber());
            generator.writeObjectField("timestamp", value.timestamp());
            generator.writeStringField("eventType", payload == null ? value.eventType() : payload.getClass().getName());
            generator.writeObjectField("payload", payload);
            generator.writeEndObject();
        }
    }

    private static final class EventEnvelopeDeserializer extends StdDeserializer<EventEnvelope> {

        private EventEnvelopeDeserializer() {
            super(EventEnvelope.class);
        }

        @Override
        public EventEnvelope deserialize(JsonParser parser,
                                         DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            String aggregateId = text(node, "aggregateId");
            long sequenceNumber = node.path("sequenceNumber").asLong();
            Instant timestamp = node.hasNonNull("timestamp")
                    ? context.readTreeAsValue(node.get("timestamp"), Instant.class)
                    : null;
            String eventType = text(node, "eventType");
            Object payload = deserializePayload(node.get("payload"), eventType, context);
            return new EventEnvelope(aggregateId, sequenceNumber, timestamp, eventType, payload);
        }

        private static Object deserializePayload(JsonNode payloadNode,
                                                 String eventType,
                                                 DeserializationContext context) throws IOException {
            if (eventType == null || eventType.isBlank() || payloadNode == null || payloadNode.isNull()) {
                return null;
            }
            if (!eventType.startsWith(ALLOWED_PACKAGE_PREFIX)) {
                throw context.weirdStringException(eventType, EventEnvelope.class,
                        "eventType hors allowlist " + ALLOWED_PACKAGE_PREFIX);
            }
            return context.readTreeAsValue(payloadNode, resolveClass(eventType));
        }

        private static Class<?> resolveClass(String eventType) throws JsonMappingException {
            try {
                return Class.forName(eventType, false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException exception) {
                throw new JsonMappingException(null, "eventType inconnu: " + eventType, exception);
            }
        }

        private static String text(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asText();
        }
    }
}
