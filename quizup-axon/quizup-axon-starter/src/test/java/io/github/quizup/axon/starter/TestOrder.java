package io.github.quizup.axon.starter;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * Agrégat de test embarqué dans le starter, utilisé par {@link AxonTestFixtureWiringTest}
 * pour démontrer le wiring 100 % in-memory du harness {@code AggregateTestFixture} +
 * {@code QuizUpAxonMatchers} (sans Spring, sans Axon Server, sans Postgres).
 */
@Aggregate
class TestOrder {

    @AggregateIdentifier
    private String orderId;
    private int quantity;

    /**
     * Constructeur par défaut requis par Axon pour la restauration par événement.
     */
    protected TestOrder() {
    }

    @CommandHandler
    public TestOrder(TestOrderCommand.PlaceOrderCommand command) {
        this.orderId = command.orderId();
        this.quantity = 0;
        apply(new TestOrderEvent.PlacedEvent(command.orderId(), command.quantity()));
    }

    @EventSourcingHandler
    private void on(TestOrderEvent.PlacedEvent event) {
        this.orderId = event.orderId();
        this.quantity = event.quantity();
    }

    public String orderId() {
        return orderId;
    }

    public int quantity() {
        return quantity;
    }
}
