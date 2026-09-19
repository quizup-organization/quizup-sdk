package io.github.quizup.axon.starter;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

/**
 * Test de wiring 100 % in-memory du harness Axon du starter.
 * <p>
 * Vérifie que le {@code AggregateTestFixture} du module {@code quizup-axon-test} + le helper
 * {@code QuizUpAxonMatchers} sont utilisables directement (sans Spring, sans Axon Server, sans
 * Postgres) : l'agrégat de test {@link TestOrder} est instancié in-memory, la commande
 * {@link TestOrderCommand.PlaceOrderCommand} est traitée, et l'événement {@link
 * TestOrderEvent.PlacedEvent} est asserté.
 * <p>
 * Ce test est le « proof of concept » du ticket S01 : il prouve que la convention
 * AggregateTestFixture + QuizUpAxonMatchers fonctionne out-of-the-box.
 */
class AxonTestFixtureWiringTest {

    private final AggregateTestFixture<TestOrder> fixture = new AggregateTestFixture<>(TestOrder.class);

    @Test
    void placeOrder_appliesPlacedEvent() {
        fixture.givenNoPriorActivity()
                .when(new TestOrderCommand.PlaceOrderCommand("order-1", 3))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        TestOrderEvent.PlacedEvent.class,
                        e -> ((TestOrderEvent.PlacedEvent) e).orderId().equals("order-1")
                                && ((TestOrderEvent.PlacedEvent) e).quantity() == 3));
    }
}
