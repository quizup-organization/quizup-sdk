package io.github.quizup.microservice.core.infrastructure.in.api.request;

import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.microservice.core.domain.model.search.SortDirection;

import java.io.Serializable;

/**
 * DTO REST pour un critère de tri.
 * Implémente {@link SortCriteria} pour être directement utilisable dans la couche domaine.
 */
public record SortRequest(
        String property,
        SortDirection direction
) implements SortCriteria, Serializable {
}

