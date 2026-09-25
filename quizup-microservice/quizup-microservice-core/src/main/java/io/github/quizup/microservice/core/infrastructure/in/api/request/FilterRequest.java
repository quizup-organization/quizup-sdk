package io.github.quizup.microservice.core.infrastructure.in.api.request;

import io.github.quizup.microservice.core.domain.model.search.FilterOperator;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de recherche — critère de filtrage (transport REST **et** bus Axon).
 */
public record FilterRequest(
        String property,
        FilterOperator operator,
        Object value,
        Object valueTo,
        List<Object> values
) implements Serializable {
}
