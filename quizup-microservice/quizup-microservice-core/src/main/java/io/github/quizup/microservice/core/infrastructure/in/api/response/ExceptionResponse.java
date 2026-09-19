package io.github.quizup.microservice.core.infrastructure.in.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Réponse standardisée pour les exceptions
 * Conforme à RFC 7807 (Problem Details for HTTP APIs)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExceptionResponse(
    String type,
    ProblemCategory category,
    String title,
    String detail,
    Map<String, Object> context,
    Instant timestamp,
    Integer status,
    String path
) implements Serializable {


    /**
     * Constructeur simplifié sans contexte
     */
    public ExceptionResponse(
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Integer status,
            String path) {
        this(type, category, title, detail, null, Instant.now(), status, path);
    }

    /**
     * Constructeur avec contexte
     */
    public ExceptionResponse(
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context,
            Integer status,
            String path) {
        this(type, category, title, detail, context, Instant.now(), status, path);
    }
}
