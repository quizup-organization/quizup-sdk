package io.github.quizup.microservice.core.domain.model.notification;

import java.io.Serializable;
import java.time.Instant;

/**
 * Enveloppe commune à toutes les notifications temps réel (WebSocket) et à leur historique REST.
 *
 * <p>Les métadonnées proviennent de l'événement Axon sous-jacent et permettent au client de
 * reconstruire son état de façon déterministe : {@code sequenceNumber} garantit l'ordre et la
 * déduplication (REST historique + push WS), {@code notificationId} l'identité stable de
 * l'événement, {@code occurredAt} l'horodatage.</p>
 *
 * @param notificationId identifiant de l'événement Axon (stable)
 * @param aggregateId    identifiant de l'agrégat source (gameId, lobbyId, challengeId…)
 * @param sequenceNumber numéro de séquence de l'événement au sein de l'agrégat
 * @param occurredAt     horodatage de l'événement
 * @param payload        la notification métier (GameNotification, LobbyNotification…)
 */
public record NotificationEnvelope<T>(
        String notificationId,
        String aggregateId,
        long sequenceNumber,
        Instant occurredAt,
        T payload
) implements Serializable {
}
