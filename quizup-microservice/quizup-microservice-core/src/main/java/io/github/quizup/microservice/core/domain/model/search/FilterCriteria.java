package io.github.quizup.microservice.core.domain.model.search;

import java.util.List;

public interface FilterCriteria {
    String property();
    FilterOperator operator();
    Object value();
    Object valueTo();
    List<Object> values();
}
