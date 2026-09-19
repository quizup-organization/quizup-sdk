package io.github.quizup.microservice.core.infrastructure.mapper;

import io.github.quizup.microservice.core.domain.model.search.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mapper pour convertir un {@link Page} Spring Data en {@link PageResult} du domaine.
 */
public final class PageResultMapper {

    private PageResultMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convertit un {@link Page} Spring Data en {@link PageResult}.
     */
    public static <T> PageResult<T> toPageResult(@NonNull Page<T> page) {
        return new DefaultPageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                toSortCriteria(page.getSort()),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    private static List<SortCriteria> toSortCriteria(Sort sort) {
        if (sort.isUnsorted()) {
            return Collections.emptyList();
        }

        return sort.stream()
                .map(PageResultMapper::toSortCriteria)
                .filter(Objects::nonNull)
                .toList();
    }

    private static SortCriteria toSortCriteria(Sort.Order order) {
        if (order == null) {
            return null;
        }

        String property = order.getProperty();
        SortDirection direction = toSortDirection(order.getDirection());

        return new DefaultSortCriteria(property, direction);

    }

    private static SortDirection toSortDirection(Sort.Direction direction) {
        return switch (direction) {
            case ASC -> SortDirection.ASC;
            case DESC -> SortDirection.DESC;
        };

    }
}
