package io.github.quizup.microservice.core.infrastructure.in.api.request;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.microservice.core.domain.model.search.SortDirection;

import java.io.Serializable;

/**
 * DTO REST pour un critère de tri.
 * Implémente {@link SortCriteria} pour être directement utilisable dans la couche domaine.
 * {@code @JsonTypeInfo(NONE)} désactive l'info de type héritée de l'interface côté REST.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public record SortRequest(
        String property,
        SortDirection direction
) implements SortCriteria, Serializable {
}

