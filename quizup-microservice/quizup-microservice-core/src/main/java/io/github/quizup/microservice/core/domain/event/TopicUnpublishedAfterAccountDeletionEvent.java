package io.github.quizup.microservice.core.domain.event;

import java.time.Instant;

/**
 * Événement transverse émis par theme quand la suppression d'un compte fait passer un sujet
 * publié sous le seuil de 7 questions approuvées. Il permet à notification de prévenir le
 * propriétaire sans coupler theme au service notification.
 */
public record TopicUnpublishedAfterAccountDeletionEvent(
        String topicId,
        String ownerId,
        String deletedUserId,
        int remainingApprovedQuestions,
        Instant occurredAt
) {
}
