package io.github.quizup.microservice.core.infrastructure.in.api.request;

import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.FilterOperator;

import java.io.Serializable;
import java.util.List;

/**
 * DTO REST pour un critère de filtrage.
 * Implémente {@link FilterCriteria} pour être directement utilisable dans la couche domaine.
 */
public record FilterRequest(
        String property,
        FilterOperator operator,
        Object value,
        Object valueTo,
        List<Object> values
) implements FilterCriteria, Serializable {
}

