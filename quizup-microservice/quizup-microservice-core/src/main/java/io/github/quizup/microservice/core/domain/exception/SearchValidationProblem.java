package io.github.quizup.microservice.core.domain.exception;

import java.util.Map;

public abstract class SearchValidationProblem extends BaseProblem {

    protected SearchValidationProblem(
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        super(type, ProblemCategory.VALIDATION, title, detail, context);
    }

    protected SearchValidationProblem(String type, String title, String detail) {
        this(type, title, detail, null);
    }

    protected SearchValidationProblem(String type, String title) {
        this(type, title, null, null);
    }
}
