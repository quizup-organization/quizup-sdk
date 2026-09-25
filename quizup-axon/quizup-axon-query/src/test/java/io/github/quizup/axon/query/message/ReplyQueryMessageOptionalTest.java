package io.github.quizup.axon.query.message;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.GenericQueryResponseMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verrouille le correctif SDK : une query {@code Optional<T>} doit rester un {@code Optional} après
 * le transport, y compris vide (avant correctif, Axon {@code forSerialization()} renvoyait l'élément
 * brut / {@code null} et le caller distant levait un NPE sur {@code optional.map(...)}).
 */
class ReplyQueryMessageOptionalTest {

    record Rank(String userId, int rank) {
    }

    private final Serializer serializer = JacksonSerializer.builder()
            .objectMapper(JsonMapper.builder().addModule(new Jdk8Module()).build())
            .build();

    @Test
    void optionalValueSurvivesWire() {
        QueryResponseMessage<?> response = new GenericQueryResponseMessage<>(Optional.of(new Rank("u", 7)));

        QueryResponseMessage<?> back = exchange(response, ResponseTypes.optionalInstanceOf(Rank.class));

        assertInstanceOf(Optional.class, back.getPayload());
        assertEquals(Optional.of(new Rank("u", 7)), back.getPayload());
    }

    @Test
    void emptyOptionalSurvivesWire() {
        QueryResponseMessage<?> response = new GenericQueryResponseMessage<>(Optional.empty());

        QueryResponseMessage<?> back = exchange(response, ResponseTypes.optionalInstanceOf(Rank.class));

        assertInstanceOf(Optional.class, back.getPayload());
        assertEquals(Optional.empty(), back.getPayload());
    }

    @Test
    void responseTypeOnWireKeepsOptionalShape() {
        ResponseType<?> wireType = ResponseTypeWire.forSerialization(ResponseTypes.optionalInstanceOf(Rank.class));

        assertInstanceOf(RawOptionalResponseType.class, wireType);
    }

    private QueryResponseMessage<?> exchange(QueryResponseMessage<?> response, ResponseType<?> responseType) {
        ReplyQueryMessage envelope = new ReplyQueryMessage("q", response, serializer, responseType);
        return envelope.getQueryResponseMessage(serializer);
    }
}
