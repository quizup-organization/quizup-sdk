package io.github.quizup.axon.starter;

/**
 * Événements de l'agrégat de test {@link TestOrder} (record imbriqué, pattern codebase).
 */
class TestOrderEvent {

    record PlacedEvent(String orderId, int quantity) {
    }
}
