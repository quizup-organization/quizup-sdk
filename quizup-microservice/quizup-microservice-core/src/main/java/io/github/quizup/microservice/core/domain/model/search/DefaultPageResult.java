package io.github.quizup.microservice.core.domain.model.search;

import java.util.List;

/**
 * Implémentation record de {@link PageResult} utilisée par le mapper.
 */
public record DefaultPageResult<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        List<SortCriteria> sorts,
        boolean first,
        boolean last,
        boolean empty
) implements PageResult<T> {

}

