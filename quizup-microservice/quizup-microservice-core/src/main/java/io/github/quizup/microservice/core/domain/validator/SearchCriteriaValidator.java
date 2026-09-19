package io.github.quizup.microservice.core.domain.validator;

import io.github.quizup.microservice.core.domain.exception.SearchValidationProblems;
import io.github.quizup.microservice.core.domain.model.search.*;

import java.util.EnumSet;
import java.util.Set;

/**
 * Validateur de {@link SearchCriteria} par rapport à un {@link SearchableEntity}.
 * Vérifie que les filtres et tris référencent des champs existants,
 * que les opérateurs sont compatibles avec les types de champs,
 * et que les valeurs requises sont présentes.
 */
public final class SearchCriteriaValidator {

    /** Opérateurs réservés aux champs texte */
    private static final Set<FilterOperator> STRING_ONLY_OPERATORS = EnumSet.of(
            FilterOperator.CONTAINS,
            FilterOperator.NOT_CONTAINS,
            FilterOperator.STARTS_WITH,
            FilterOperator.ENDS_WITH
    );

    /** Opérateurs réservés aux champs numériques ou date (comparables ordonnés) */
    private static final Set<FilterOperator> COMPARABLE_ONLY_OPERATORS = EnumSet.of(
            FilterOperator.LESS_THAN,
            FilterOperator.GREATER_THAN,
            FilterOperator.BETWEEN
    );

    private SearchCriteriaValidator() {
    }

    /**
     * Valide un {@link SearchCriteria} par rapport au descripteur de l'entité recherchable.
     *
     * @param entity   le descripteur de l'entité (champs autorisés)
     * @param criteria les critères de recherche à valider
     * @throws SearchValidationProblems.UnknownFieldProblem        si un champ référencé n'existe pas
     * @throws SearchValidationProblems.IncompatibleOperatorProblem si un opérateur est incompatible avec le type
     * @throws SearchValidationProblems.MissingValueProblem        si une valeur requise est absente
     */
    public static void validate(SearchableEntity entity, SearchCriteria criteria) {
        if (criteria.filters() != null) {
            for (FilterCriteria filter : criteria.filters()) {
                validateFilter(entity, filter);
            }
        }

        if (criteria.sorts() != null) {
            for (SortCriteria sort : criteria.sorts()) {
                validateSort(entity, sort);
            }
        }
    }

    private static void validateFilter(SearchableEntity entity, FilterCriteria filter) {
        String fieldKey = filter.property();
        FilterOperator operator = filter.operator();

        // 1. Vérifier que le champ existe
        SearchableField field = entity.findByKey(fieldKey)
                .orElseThrow(() -> new SearchValidationProblems.UnknownFieldProblem(fieldKey));

        // 2. Vérifier la compatibilité opérateur / type de champ
        validateOperatorCompatibility(fieldKey, operator, field.type());

        // 3. Vérifier la présence des valeurs requises
        validateRequiredValues(fieldKey, operator, filter);
    }

    private static void validateOperatorCompatibility(String fieldKey, FilterOperator operator, FieldType fieldType) {
        // Les opérateurs texte ne sont valides que pour STRING
        if (STRING_ONLY_OPERATORS.contains(operator) && fieldType != FieldType.STRING) {
            throw new SearchValidationProblems.IncompatibleOperatorProblem(fieldKey, operator, fieldType);
        }

        // Les opérateurs de comparaison ordonnée ne sont valides que pour NUMBER et DATE
        if (COMPARABLE_ONLY_OPERATORS.contains(operator)
                && fieldType != FieldType.NUMBER
                && fieldType != FieldType.DATE) {
            throw new SearchValidationProblems.IncompatibleOperatorProblem(fieldKey, operator, fieldType);
        }
    }

    private static void validateRequiredValues(String fieldKey, FilterOperator operator, FilterCriteria filter) {
        switch (operator) {
            case BETWEEN -> {
                if (filter.value() == null) {
                    throw new SearchValidationProblems.MissingValueProblem(fieldKey, operator, "value");
                }
                if (filter.valueTo() == null) {
                    throw new SearchValidationProblems.MissingValueProblem(fieldKey, operator, "valueTo");
                }
            }
            case IN, NOT_IN -> {
                if (filter.values() == null || filter.values().isEmpty()) {
                    throw new SearchValidationProblems.MissingValueProblem(fieldKey, operator, "values");
                }
            }
            case BLANK, NOT_BLANK -> {
                // Pas de valeur requise
            }
            default -> {
                if (filter.value() == null) {
                    throw new SearchValidationProblems.MissingValueProblem(fieldKey, operator, "value");
                }
            }
        }
    }

    private static void validateSort(SearchableEntity entity, SortCriteria sort) {
        String fieldKey = sort.property();
        entity.findByKey(fieldKey)
                .orElseThrow(() -> new SearchValidationProblems.UnknownFieldProblem(fieldKey));
    }
}

