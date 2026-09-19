package io.github.quizup.axon.starter;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Commande de l'agrégat de test {@link TestOrder} (record imbriqué, pattern codebase).
 */
class TestOrderCommand {

    record PlaceOrderCommand(
            @TargetAggregateIdentifier String orderId,
            int quantity
    ) {
    }
}
