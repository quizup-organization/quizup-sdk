package io.github.quizup.microservice.core.domain.model.search;

public record DefaultSortCriteria(
        String property,
        SortDirection direction
) implements SortCriteria {
}
