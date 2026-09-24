package io.github.quizup.microservice.core.infrastructure.in.api.request;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.quizup.microservice.core.domain.model.search.PageCriteria;

import java.io.Serializable;

/**
 * DTO REST pour les critères de pagination.
 * Implémente {@link PageCriteria} pour être directement utilisable dans la couche domaine.
 * {@code @JsonTypeInfo(NONE)} désactive l'info de type héritée de l'interface (utile sur le bus,
 * inutile — et bloquante — sur le corps REST).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public record PageRequest(
        Integer number,
        Integer size
) implements PageCriteria, Serializable {
}

