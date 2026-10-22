package io.github.quizup.microservice.core.domain.event;

import java.time.Instant;

/**
 * Événement transverse de suppression de compte, diffusé par identity à tous les services.
 *
 * <p>Il ne porte aucune donnée personnelle (pas d'email, pas de pseudonyme) : uniquement
 * l'identifiant technique et la clé d'audit de la suppression. Chaque service applique ensuite
 * ses propres règles de suppression/anonymisation de manière idempotente.</p>
 */
public record UserAccountDeletedEvent(
        String userId,
        String deletionId,
        Instant deletedAt
) {
}
