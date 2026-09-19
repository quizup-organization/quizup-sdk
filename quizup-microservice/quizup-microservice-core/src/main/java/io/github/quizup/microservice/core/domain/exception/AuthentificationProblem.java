package io.github.quizup.microservice.core.domain.exception;

import java.util.Map;

public abstract class AuthentificationProblem extends BaseProblem {

    protected AuthentificationProblem(
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        super(type, ProblemCategory.PERMISSION, title, detail, context);
    }

    protected AuthentificationProblem(String type, String title, String detail) {
        this(type, title, detail, null);
    }

    protected AuthentificationProblem(String type, String title) {
        this(type, title, null, null);
    }
}
