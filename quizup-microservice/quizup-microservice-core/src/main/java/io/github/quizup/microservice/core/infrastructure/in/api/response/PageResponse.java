package io.github.quizup.microservice.core.infrastructure.in.api.response;

import java.io.Serializable;
import java.util.List;

/**
 * Réponse REST générique pour les résultats de recherche paginée.
 */
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        List<SortResponse> sorts,
        boolean first,
        boolean last,
        boolean empty
) implements Serializable {
}
