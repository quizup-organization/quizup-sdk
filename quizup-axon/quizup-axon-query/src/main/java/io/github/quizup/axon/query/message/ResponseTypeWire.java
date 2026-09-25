package io.github.quizup.axon.query.message;

import org.axonframework.messaging.responsetypes.OptionalResponseType;
import org.axonframework.messaging.responsetypes.ResponseType;

/**
 * Normalise un {@link ResponseType} avant sérialisation sur le fil.
 *
 * <p>Axon {@code OptionalResponseType#forSerialization()} perd l'optional : elle est remplacée par
 * {@link RawOptionalResponseType} (sérialisable) afin que le caller distant récupère bien un
 * {@link java.util.Optional}. Les autres types gardent leur {@code forSerialization()} habituel.</p>
 */
final class ResponseTypeWire {

    private ResponseTypeWire() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static ResponseType<?> forSerialization(ResponseType<?> responseType) {
        if (responseType instanceof OptionalResponseType optionalResponseType) {
            return new RawOptionalResponseType(optionalResponseType.getExpectedResponseType());
        }
        return (responseType == null) ? null : responseType.forSerialization();
    }
}
