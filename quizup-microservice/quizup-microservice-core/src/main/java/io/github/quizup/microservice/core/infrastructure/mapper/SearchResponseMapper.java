package io.github.quizup.microservice.core.infrastructure.mapper;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SortResponse;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;


public final class SearchResponseMapper {

    private SearchResponseMapper() {

    }

    public static <I,O> PageResponse<O> toSearchResponse(PageResult<I> pageResult, Function<I, O> mapper) {
        if (pageResult == null) {
            return toSearchResponse(PageResult.unpaged());
        }

        List<O> mappedContent = pageResult.content().stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .toList();

        return new PageResponse<>(
                mappedContent,
                pageResult.pageNumber(),
                pageResult.pageSize(),
                pageResult.totalElements(),
                pageResult.totalPages(),
                toSortResponses(pageResult.sorts()),
                pageResult.first(),
                pageResult.last(),
                pageResult.empty()
        );
    }
    public static <T> PageResponse<T> toSearchResponse(PageResult<T> pageResult) {
        if (pageResult == null) {
            return toSearchResponse(PageResult.unpaged());
        }

        return new PageResponse<>(
                pageResult.content(),
                pageResult.pageNumber(),
                pageResult.pageSize(),
                pageResult.totalElements(),
                pageResult.totalPages(),
                toSortResponses(pageResult.sorts()),
                pageResult.first(),
                pageResult.last(),
                pageResult.empty()
        );
    }

    private static List<SortResponse> toSortResponses(List<SortCriteria> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Collections.emptyList();
        }

        return sorts.stream()
                .map(SearchResponseMapper::toSortResponse)
                .filter(Objects::nonNull)
                .toList();
    }

    private static SortResponse toSortResponse(SortCriteria sortCriteria) {
        if (sortCriteria == null) {
            return null;
        }

        return new SortResponse(sortCriteria.property(), sortCriteria.direction());
    }
}
