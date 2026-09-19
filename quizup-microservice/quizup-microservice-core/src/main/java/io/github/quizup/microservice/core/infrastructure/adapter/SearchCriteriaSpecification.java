package io.github.quizup.microservice.core.infrastructure.adapter;

import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.domain.model.search.SearchableEntity;
import io.github.quizup.microservice.core.domain.model.search.SearchableField;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Convertit un {@link SearchCriteria} en {@link Specification} JPA
 * en utilisant le {@link SearchableEntity} pour la résolution des champs
 * et le registry de {@link PredicateBuilder} pour la construction des prédicats.
 */
public class SearchCriteriaSpecification<T> implements Specification<T> {

    private final SearchCriteria criteria;
    private final SearchableEntity searchableEntity;

    public SearchCriteriaSpecification(SearchCriteria criteria, SearchableEntity searchableEntity) {
        this.criteria = criteria;
        this.searchableEntity = searchableEntity;
    }

    @Nullable
    @Override
    public Predicate toPredicate(@NonNull Root<T> root, @Nullable CriteriaQuery<?> query, @NonNull CriteriaBuilder criteriaBuilder) {
        Predicate predicate = criteriaBuilder.conjunction();

        if (criteria.filters() == null || criteria.filters().isEmpty()) {
            return predicate;
        }

        for (FilterCriteria filter : criteria.filters()) {

            SearchableField field = searchableEntity.getByKey(filter.property());

            PredicateBuilder builder = PredicateBuilder.REGISTRY.get(filter.operator());

            if (builder != null) {
                predicate = builder.build(root, criteriaBuilder, predicate, field, filter);
            }
        }

        return predicate;
    }
}
