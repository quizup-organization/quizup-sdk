package io.github.quizup.microservice.core.infrastructure.mapper;

import io.github.quizup.microservice.core.domain.model.search.*;
import io.github.quizup.microservice.core.infrastructure.in.api.request.FilterRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.PageRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SortRequest;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SearchRequestMapper {

    private SearchRequestMapper() {
        // Constructeur privé pour empêcher l'instanciation
    }


    public static SearchCriteria toSearchCriteria(SearchRequest searchRequest) {
        if (searchRequest == null) {
            return SearchCriteria.empty();
        }

        final List<FilterCriteria> filters = Optional.of(searchRequest)
                .map(SearchRequest::filters)
                .orElse(Collections.emptyList())
                .stream()
                .map(SearchRequestMapper::toFilterCriteria)
                .filter(Objects::nonNull)
                .toList();

        final List<SortCriteria> sorts = Optional.of(searchRequest)
                .map(SearchRequest::sorts)
                .orElse(Collections.emptyList())
                .stream()
                .map(SearchRequestMapper::toSortCriteria)
                .filter(Objects::nonNull)
                .toList();

        final PageCriteria page = Optional.of(searchRequest)
                .map(SearchRequest::page)
                .map(SearchRequestMapper::toPageCriteria)
                .orElse(PageCriteria.unpaged());

        return new DefaultSearchCriteria(filters, sorts, page);
    }


    private static PageCriteria toPageCriteria(PageRequest pageRequest) {
        if (pageRequest == null) {
            return PageCriteria.unpaged();
        }

        // PageRequest implémente déjà l'interface domaine PageCriteria.
        return pageRequest;
    }

    public static FilterCriteria toFilterCriteria(FilterRequest searchCriteria) {
        if (searchCriteria == null) {
            return null;
        }

        // FilterRequest implémente déjà l'interface domaine FilterCriteria.
        return searchCriteria;
    }


    public static SortCriteria toSortCriteria(SortRequest sortRequest) {
        if (sortRequest == null) {
            return null;
        }

        // SortRequest implémente déjà l'interface domaine SortCriteria.
        return sortRequest;
    }
}
