package io.github.quizup.microservice.core.infrastructure.in.api.response;

import io.github.quizup.microservice.core.domain.model.search.SortDirection;

import java.io.Serializable;

public record SortResponse(
        String property,
        SortDirection direction
) implements Serializable {
}
