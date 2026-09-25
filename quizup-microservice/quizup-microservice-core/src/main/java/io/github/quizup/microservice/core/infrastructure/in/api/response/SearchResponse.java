package io.github.quizup.microservice.core.infrastructure.in.api.response;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * Résultat d'une recherche paginée (DTO de transport REST **et** bus Axon).
 */
public record SearchResponse<T>(
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

    /** Réécrit le contenu (mapping d'entité/domaine → DTO) en conservant la pagination. */
    public <O> SearchResponse<O> map(Function<T, O> mapper) {
        return new SearchResponse<>(
                content().stream().map(mapper).toList(),
                pageNumber(),
                pageSize(),
                totalElements(),
                totalPages(),
                sorts(),
                first(),
                last(),
                empty()
        );
    }
}
