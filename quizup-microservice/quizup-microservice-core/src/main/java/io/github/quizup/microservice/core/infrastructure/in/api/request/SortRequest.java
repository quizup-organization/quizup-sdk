package io.github.quizup.microservice.core.infrastructure.in.api.request;

import io.github.quizup.microservice.core.domain.model.search.SortDirection;

import java.io.Serializable;

/**
 * DTO de recherche — critère de tri (transport REST **et** bus Axon).
 */
public record SortRequest(
        String property,
        SortDirection direction
) implements Serializable {
}
