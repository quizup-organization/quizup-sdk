package io.github.quizup.microservice.core.domain.exception;

import java.util.Map;

/**
 * Base des problèmes liés aux langues supportées.
 */
public abstract class LanguageProblem extends BaseProblem {

    protected LanguageProblem(
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        super(type, ProblemCategory.VALIDATION, title, detail, context);
    }
}
