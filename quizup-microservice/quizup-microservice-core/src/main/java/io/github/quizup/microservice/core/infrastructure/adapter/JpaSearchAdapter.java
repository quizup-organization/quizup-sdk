package io.github.quizup.microservice.core.infrastructure.adapter;

import io.github.quizup.microservice.core.domain.model.search.SearchableEntity;
import io.github.quizup.microservice.core.domain.model.search.SortDirection;
import io.github.quizup.microservice.core.infrastructure.in.api.request.PageRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SortRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SortResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Adaptateur JPA générique pour la recherche dynamique : reçoit un {@link SearchRequest}
 * (DTO de transport), valide les critères, construit la spécification JPA et retourne
 * un {@link SearchResponse} (DTO de transport). Aucun modèle de pagination custom.
 */
public class JpaSearchAdapter<T> {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final SearchableEntity searchableEntity;
    private final JpaSpecificationExecutor<T> delegate;

    public JpaSearchAdapter(JpaSpecificationExecutor<T> delegate, SearchableEntity searchableEntity) {
        this.delegate = delegate;
        this.searchableEntity = searchableEntity;
    }

    public SearchResponse<T> findAll(SearchRequest request) {
        SearchRequestValidator.validate(searchableEntity, request);

        Specification<T> specification = new SearchRequestSpecification<>(request, searchableEntity);
        Page<T> page = delegate.findAll(specification, toPageable(request));

        return toSearchResponse(page);
    }

    private static Pageable toPageable(SearchRequest request) {
        if (request == null || request.page() == null) {
            return Pageable.unpaged();
        }

        PageRequest page = request.page();
        int number = page.number() == null ? 0 : page.number();
        int size = page.size() == null ? DEFAULT_PAGE_SIZE : page.size();

        return org.springframework.data.domain.PageRequest.of(number, size, toSort(request.sorts()));
    }

    private static Sort toSort(List<SortRequest> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = sorts.stream()
                .filter(sort -> sort != null && sort.property() != null && !sort.property().isBlank())
                .map(sort -> sort.direction() == SortDirection.DESC
                        ? Sort.Order.desc(sort.property())
                        : Sort.Order.asc(sort.property()))
                .toList();

        return Sort.by(orders);
    }

    private static <O> SearchResponse<O> toSearchResponse(Page<O> page) {
        List<SortResponse> sorts = page.getSort().stream()
                .map(order -> new SortResponse(
                        order.getProperty(),
                        order.isAscending() ? SortDirection.ASC : SortDirection.DESC))
                .toList();

        return new SearchResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                sorts,
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
