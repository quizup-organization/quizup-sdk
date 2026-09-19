package io.github.quizup.microservice.core.domain.model.search;

public interface SortCriteria {
    String property();

    SortDirection direction();
}
