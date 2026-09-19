package io.github.quizup.microservice.core.infrastructure.in.api.request;

import io.github.quizup.microservice.core.domain.model.search.PageCriteria;

import java.io.Serializable;

/**
 * DTO REST pour les critères de pagination.
 * Implémente {@link PageCriteria} pour être directement utilisable dans la couche domaine.
 */
public record PageRequest(
        Integer number,
        Integer size
) implements PageCriteria, Serializable {
}

