package io.github.quizup.axon.query.message;

import io.github.quizup.microservice.core.domain.model.search.SortCriteria;

import java.io.Serializable;
import java.util.List;

/**
 * Représentation de transport d'un {@code PageResult<T>} sur le bus de requêtes HTTP.
 *
 * <p>Le payload d'une query paginée est un {@code PageResult<T>} : le type de {@code T} est effacé
 * au runtime, donc une désérialisation Jackson « brute » produirait des {@code LinkedHashMap}.
 * On transporte explicitement la liste typée {@code List<T>} et le nom de la classe d'élément
 * (porté par {@link ReplyQueryMessage}) pour reconstruire un {@code DefaultPageResult<T>} typé
 * côté appelant.</p>
 */
public record PageResultTransport<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        List<SortCriteria> sorts,
        boolean first,
        boolean last,
        boolean empty
) implements Serializable {
}
