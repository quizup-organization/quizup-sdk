package io.github.quizup.microservice.core.domain.model.search;

import io.github.quizup.microservice.core.domain.exception.SearchValidationProblems;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Descripteur d'une entité avec champs searchable — expose les champs autorisés pour le filtrage et le tri.
 * Chaque module métier déclare son propre enum implémentant cette interface.
 */
public interface SearchableEntity {

    /**
     * Liste des champs filtrables/triables de l'entité
     */
    List<SearchableField> fields();

    default SearchableField getByKey(String key) throws SearchValidationProblems.UnknownFieldProblem {
        return findByKey(key)
                .orElseThrow(() -> new SearchValidationProblems.UnknownFieldProblem(key));
    }

    /**
     * Recherche un champ par sa clé
     */
    default Optional<SearchableField> findByKey(String key) {
        if (StringUtils.isBlank(key)) {
            return Optional.empty();
        }

        return fields().stream()
                .filter(field -> field.matches(key))
                .findFirst();
    }
}

