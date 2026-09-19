package io.github.quizup.microservice.core.infrastructure.in.api.request;

import java.io.Serializable;
import java.util.List;

/**
 * DTO REST pour une requête de recherche dynamique.
 * Peut être utilisé comme {@code @RequestBody} dans les controllers REST.
 */
public record SearchRequest(
        List<FilterRequest> filters,
        List<SortRequest> sorts,
        PageRequest page
) implements Serializable {
}
