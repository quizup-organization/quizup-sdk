package io.github.quizup.microservice.core.infrastructure.adapter;

import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchableEntity;
import io.github.quizup.microservice.core.domain.port.out.SearchPort;
import io.github.quizup.microservice.core.domain.validator.SearchCriteriaValidator;
import io.github.quizup.microservice.core.infrastructure.mapper.PageCriteriaMapper;
import io.github.quizup.microservice.core.infrastructure.mapper.PageResultMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Adaptateur JPA générique pour la recherche dynamique.
 * Valide les critères, construit la spécification JPA, et retourne le résultat paginé.
 */
public class JpaSearchAdapter<T> implements SearchPort<T> {

    private final SearchableEntity searchableEntity;
    private final JpaSpecificationExecutor<T> delegate;

    public JpaSearchAdapter(JpaSpecificationExecutor<T> delegate, SearchableEntity searchableEntity) {
        this.delegate = delegate;
        this.searchableEntity = searchableEntity;
    }

    @Override
    public PageResult<T> findAll(SearchCriteria criteria) {
        // 1. Validation des critères par rapport au descripteur de l'entité
        SearchCriteriaValidator.validate(searchableEntity, criteria);

        // 2. Construction de la spécification JPA
        final Specification<T> specification = new SearchCriteriaSpecification<>(criteria, searchableEntity);

        // 3. Construction du Pageable
        final Pageable pageable = PageCriteriaMapper.toPageable(criteria);

        // 4. Exécution de la requête
        final Page<T> page = delegate.findAll(specification, pageable);

        // 5. Mapping du résultat
        return PageResultMapper.toPageResult(page);
    }
}
