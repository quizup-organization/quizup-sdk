package io.github.quizup.microservice.core.domain.exception;

import io.github.quizup.microservice.core.domain.model.search.FieldType;
import io.github.quizup.microservice.core.domain.model.search.FilterOperator;

import java.util.Map;

/**
 * Exceptions de validation liées à la recherche dynamique.
 * Levées lorsqu'un {@code SearchRequest}
 * contient des filtres ou tris invalides par rapport au descripteur de l'entité recherchable.
 */
public interface SearchValidationProblems {

    class UnknownFieldProblem extends SearchValidationProblem {
        public UnknownFieldProblem(String fieldKey) {
            super(
                    "urn:quizup:search:unknownField",
                    "Unknown search field",
                    "The field '" + fieldKey + "' is not a valid searchable property",
                    Map.of("field", fieldKey)
            );
        }
    }

    class IncompatibleOperatorProblem extends SearchValidationProblem {
        public IncompatibleOperatorProblem(String fieldKey, FilterOperator operator, FieldType fieldType) {
            super(
                    "urn:quizup:search:incompatibleOperator",
                    "Incompatible filter operator",
                    "The operator '" + operator + "' is not compatible with field '" + fieldKey + "' of type " + fieldType,
                    Map.of("field", fieldKey, "operator", operator.name(), "fieldType", fieldType.name())
            );
        }
    }

    class MissingValueProblem extends SearchValidationProblem {
        public MissingValueProblem(String fieldKey, FilterOperator operator, String missingField) {
            super(
                    "urn:quizup:search:missingValue",
                    "Missing filter value",
                    "The operator '" + operator + "' on field '" + fieldKey + "' requires '" + missingField + "' to be provided",
                    Map.of("field", fieldKey, "operator", operator.name(), "missing", missingField)
            );
        }
    }
}

