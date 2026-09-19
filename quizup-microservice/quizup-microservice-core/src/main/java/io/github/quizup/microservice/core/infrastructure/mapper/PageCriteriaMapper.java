package io.github.quizup.microservice.core.infrastructure.mapper;

import io.github.quizup.microservice.core.domain.model.search.PageCriteria;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNullElse;

public final class PageCriteriaMapper {
    public static final Integer DEFAULT_PAGE_NUMBER = 0;
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    private PageCriteriaMapper() {
        // Private constructor to prevent instantiation
    }

    public static Pageable toPageable(@Nullable SearchCriteria searchCriteria) {
        if (searchCriteria == null) {
            return toPageable(null, null);
        } else {
            return toPageable(searchCriteria.page(), searchCriteria.sorts());
        }
    }

    @NonNull
    public static Pageable toPageable(@Nullable PageCriteria pageCriteria, @Nullable List<SortCriteria> sortCriteria) {
        if (pageCriteria == null) {
            return Pageable.unpaged();
        }

        Integer pageNumber = requireNonNullElse(pageCriteria.number(), DEFAULT_PAGE_NUMBER);
        Integer pageSize = requireNonNullElse(pageCriteria.size(), DEFAULT_PAGE_SIZE);

        Sort sort = SortCriteriaMapper.toSort(sortCriteria);

        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
