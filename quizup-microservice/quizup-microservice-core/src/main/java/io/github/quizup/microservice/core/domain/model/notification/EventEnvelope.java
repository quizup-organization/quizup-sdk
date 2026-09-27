package io.github.quizup.microservice.core.domain.model.notification;

import java.io.Serializable;
import java.time.Instant;

/**
 * Enveloppe d'événement transportée sur le query bus distribué (historique REST et push WebSocket).
 *
 * <p>Le payload est volontairement non typé ({@link Object}) : l'enveloppe porte {@code eventType}
 * (nom qualifié complet de la classe du payload) pour que le transport puisse le désérialiser avec
 * son type concret, sans annotation framework dans les modules domaine. Le payload d'une enveloppe
 * est toujours un objet du domaine ou un DTO interne ({@code io.github.quizup.*}) ; la
 * désérialisation est restreinte à cette allowlist par le serializer du query bus.</p>
 *
 * <p>Le contrat exposé au web n'est pas cette enveloppe : le BFF possède son propre
 * {@code EventEnvelopeResponse} (annotations Jackson du contrat web).</p>
 */
public record EventEnvelope(
        String aggregateId,
        long sequenceNumber,
        Instant timestamp,
        String eventType,
        Object payload
) implements Serializable {

    /**
     * Construit une enveloppe en dérivant {@code eventType} du type runtime du payload.
     */
    public static EventEnvelope of(String aggregateId, long sequenceNumber, Instant timestamp, Object payload) {
        return new EventEnvelope(
                aggregateId,
                sequenceNumber,
                timestamp,
                payload == null ? null : payload.getClass().getName(),
                payload
        );
    }
}
