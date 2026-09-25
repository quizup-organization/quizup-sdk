package io.github.quizup.axon.query.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.axonframework.messaging.responsetypes.OptionalResponseType;
import org.axonframework.messaging.responsetypes.ResponseType;

/**
 * Variante sérialisable de {@link OptionalResponseType}.
 *
 * <p>Axon {@code OptionalResponseType#forSerialization()} dégrade l'optional en
 * {@code instanceOf(expectedType)} : au retour du transport, le payload n'est plus un
 * {@link java.util.Optional} mais l'élément brut (ou {@code null}), ce qui casse les callers
 * distants qui attendent un {@code Optional} (ex. {@code QueryResponseTypes.optionalInstanceOf}).
 * Cette implémentation expose un {@code @JsonCreator} (comme
 * {@code RawMultipleInstancesResponseType}) et conserve l'optional sur le fil.</p>
 */
public class RawOptionalResponseType<R> extends OptionalResponseType<R> {

    @JsonCreator
    public RawOptionalResponseType(
            @JsonProperty("expectedResponseType") Class<R> expectedResponseType) {
        super(expectedResponseType);
    }

    @Override
    public ResponseType<?> forSerialization() {
        return this;
    }
}
