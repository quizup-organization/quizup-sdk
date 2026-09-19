package io.github.quizup.microservice.core.domain.exception;

import java.util.Map;

public interface SearchableProblems {

    class NoSearchableFieldsFoundProblem extends SearchableProblem {
        public NoSearchableFieldsFoundProblem(Class<?> clazz) {
            super(
                    "urn:quizup:search:noSearchableFieldsFound",
                    "No @Searchable fields found",
                    "The class '" + clazz.getName() + "' is not a valid, no fields annotated with @Searchable",
                    Map.of("class", clazz,
                            "className", clazz.getSimpleName())
            );
        }
    }
}
