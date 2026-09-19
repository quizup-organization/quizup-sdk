package io.github.quizup.microservice.core.infrastructure.mapper;

import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.microservice.core.domain.model.search.SortDirection;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;

public final class SortCriteriaMapper {

    private SortCriteriaMapper() {
    }

    @NonNull
    public static Sort toSort(@Nullable List<SortCriteria> sortCriteria) {
        if (sortCriteria == null || sortCriteria.isEmpty()) {
            return Sort.unsorted();
        }

        final List<Sort.Order> orders = sortCriteria.stream()
                .map(SortCriteriaMapper::toOrder)
                .filter(Objects::nonNull)
                .toList();

        return Sort.by(orders);
    }


    @Nullable
    public static Sort.Order toOrder(@Nullable SortCriteria sortCriteria) {
        if (sortCriteria == null) {
            return null;
        }

        String property = sortCriteria.property();

        if (StringUtils.isBlank(property)) {
            return null;
        }

        Sort.Direction direction = toDirection(sortCriteria.direction());

        return new Sort.Order(direction, property);

    }

    @Nullable
    public static Sort.Direction toDirection(@Nullable SortDirection sortDirection) {
        if (sortDirection == null) {
            return null;
        }

        return switch (sortDirection) {
            case ASC -> Sort.Direction.ASC;
            case DESC -> Sort.Direction.DESC;
        };
    }
}
