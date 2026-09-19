package io.github.quizup.microservice.core.domain.port.out;

import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageResult;

import java.util.function.Function;

/**
 * Port sortant — exécution de la recherche
 * Implémenté par chaque adaptateur technique (JPA, Mongo, Elasticsearch)
 */
public interface SearchPort<T> {

    PageResult<T> findAll(SearchCriteria criteria);

    default <O> PageResult<O> findAll(SearchCriteria criteria, Function<T, O> mapper){
        PageResult<T> page = findAll(criteria);
        return PageResult.of(page, mapper);
    }
}
