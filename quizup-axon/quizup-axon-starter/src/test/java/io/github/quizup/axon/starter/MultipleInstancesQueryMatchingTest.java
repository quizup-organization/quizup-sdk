package io.github.quizup.axon.starter;

import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.GenericQueryMessage;
import org.axonframework.queryhandling.NoHandlerForQueryException;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.axonframework.queryhandling.SimpleQueryBus;
import org.axonframework.queryhandling.annotation.AnnotationQueryHandlerAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prouve la limite d'Axon et le correctif SDK pour les queries retournant une liste d'éléments
 * génériques ({@code List<Wrapper<Payload>>}).
 *
 * <p>Axon ({@code ResponseTypes.multipleInstancesOf}) exige que l'argument de collection soit un
 * {@link Class}, et ne reconnaît donc pas un élément paramétré : le handler est introuvable. La
 * factory SDK {@link QueryResponseTypes#multipleInstancesOf(Class)} matche le <b>type brut</b> et
 * résout le handler.</p>
 */
class MultipleInstancesQueryMatchingTest {

    private final SimpleQueryBus queryBus = SimpleQueryBus.builder().build();

    @BeforeEach
    void registerHandler() {
        new AnnotationQueryHandlerAdapter<>(new WrapperHandler()).subscribe(queryBus);
    }

    @Test
    void axonResponseTypesDoesNotMatchGenericElement() {
        QueryMessage<WrapperQuery, List<Wrapper<Payload>>> query = new GenericQueryMessage<>(
                new WrapperQuery(),
                stockMultipleInstancesResponseType()
        );

        CompletionException failure = assertThrows(
                CompletionException.class,
                () -> queryBus.query(query).join()
        );

        assertTrue(
                failure.getCause() instanceof NoHandlerForQueryException,
                "Axon ne doit pas résoudre un handler de List<Wrapper<Payload>> via multipleInstancesOf"
        );
    }

    @Test
    void queryResponseTypesMatchesGenericElement() {
        QueryMessage<WrapperQuery, List<Wrapper<Payload>>> query = new GenericQueryMessage<>(
                new WrapperQuery(),
                sdkMultipleInstancesResponseType()
        );

        QueryResponseMessage<List<Wrapper<Payload>>> response = queryBus.query(query).join();

        assertEquals(2, response.getPayload().size());
        assertEquals("a", response.getPayload().get(0).payload().value());
        assertEquals("b", response.getPayload().get(1).payload().value());
    }

    @SuppressWarnings("unchecked")
    private static ResponseType<List<Wrapper<Payload>>> stockMultipleInstancesResponseType() {
        return (ResponseType<List<Wrapper<Payload>>>) (ResponseType<?>)
                ResponseTypes.multipleInstancesOf(Wrapper.class);
    }

    @SuppressWarnings("unchecked")
    private static ResponseType<List<Wrapper<Payload>>> sdkMultipleInstancesResponseType() {
        return (ResponseType<List<Wrapper<Payload>>>) (ResponseType<?>)
                QueryResponseTypes.multipleInstancesOf(Wrapper.class);
    }

    record WrapperQuery() {
    }

    record Payload(String value) {
    }

    record Wrapper<T>(T payload) {
    }

    static class WrapperHandler {

        @QueryHandler
        public List<Wrapper<Payload>> handle(WrapperQuery query) {
            return List.of(
                    new Wrapper<>(new Payload("a")),
                    new Wrapper<>(new Payload("b"))
            );
        }
    }
}
