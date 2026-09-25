package io.github.quizup.microservice.core.infrastructure.adapter;

import io.github.quizup.microservice.core.infrastructure.in.api.request.FilterRequest;
import io.github.quizup.microservice.core.domain.model.search.FilterOperator;
import io.github.quizup.microservice.core.domain.model.search.SearchableField;
import io.github.quizup.microservice.core.domain.model.search.FieldType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stratégie de construction de prédicat JPA pour un opérateur de filtrage donné.
 * Le registry statique {@link #REGISTRY} associe chaque {@link FilterOperator} à son implémentation.
 */
public interface PredicateBuilder {

    /**
     * Registry statique — associe chaque opérateur à son builder
     */
    Map<FilterOperator, PredicateBuilder> REGISTRY = buildRegistry();

    /**
     * Construit un prédicat JPA à partir d'un filtre, en le combinant (AND) au prédicat existant.
     *
     * @param root            la racine JPA de la requête
     * @param criteriaBuilder le CriteriaBuilder JPA
     * @param predicate       le prédicat accumulé jusqu'ici
     * @param field           le descripteur du champ recherchable
     * @param filter          le critère de filtrage complet (property, operator, value, valueTo, values)
     * @return le nouveau prédicat combiné
     */
    <T> Predicate build(Root<T> root, CriteriaBuilder criteriaBuilder, Predicate predicate, SearchableField field, FilterRequest filter);

    // ─── Implémentations ────────────────────────────────────────────────

    class EqualsPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            Object value = ValueResolver.resolve(filter.value(), field.type());
            return cb.and(predicate, cb.equal(root.get(field.key()), value));
        }
    }

    class NotEqualsPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            Object value = ValueResolver.resolve(filter.value(), field.type());
            return cb.and(predicate, cb.notEqual(root.get(field.key()), value));
        }
    }

    class LessThanPredicateBuilder implements PredicateBuilder {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            Object value = ValueResolver.resolve(filter.value(), field.type());
            return switch (field.type()) {
                case DATE -> {
                    Expression<Instant> dateKey = root.get(field.key());
                    yield cb.and(predicate, cb.lessThan(dateKey, (Instant) value));
                }
                case NUMBER -> {
                    Expression<Number> numKey = root.get(field.key());
                    yield cb.and(predicate, cb.lt(numKey, (Number) value));
                }
                default -> {
                    Expression<Comparable<Object>> key = root.get(field.key());
                    yield cb.and(predicate, cb.lessThan(key, (Comparable<Object>) value));
                }
            };
        }
    }

    class GreaterThanPredicateBuilder implements PredicateBuilder {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            Object value = ValueResolver.resolve(filter.value(), field.type());
            return switch (field.type()) {
                case DATE -> {
                    Expression<Instant> dateKey = root.get(field.key());
                    yield cb.and(predicate, cb.greaterThan(dateKey, (Instant) value));
                }
                case NUMBER -> {
                    Expression<Number> numKey = root.get(field.key());
                    yield cb.and(predicate, cb.gt(numKey, (Number) value));
                }
                default -> {
                    Expression<Comparable<Object>> key = root.get(field.key());
                    yield cb.and(predicate, cb.greaterThan(key, (Comparable<Object>) value));
                }
            };
        }
    }

    class ContainsPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            String value = String.valueOf(filter.value()).toLowerCase();
            return cb.and(predicate, cb.like(cb.lower(root.get(field.key())), "%" + value + "%"));
        }
    }

    class NotContainsPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            String value = String.valueOf(filter.value()).toLowerCase();
            return cb.and(predicate, cb.notLike(cb.lower(root.get(field.key())), "%" + value + "%"));
        }
    }

    class StartsWithPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            String value = String.valueOf(filter.value()).toLowerCase();
            return cb.and(predicate, cb.like(cb.lower(root.get(field.key())), value + "%"));
        }
    }

    class EndsWithPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            String value = String.valueOf(filter.value()).toLowerCase();
            return cb.and(predicate, cb.like(cb.lower(root.get(field.key())), "%" + value));
        }
    }

    class InPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            List<Object> values = ValueResolver.resolveAll(filter.values(), field.type());
            return cb.and(predicate, root.get(field.key()).in(values));
        }
    }

    class NotInPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            List<Object> values = ValueResolver.resolveAll(filter.values(), field.type());
            return cb.and(predicate, cb.not(root.get(field.key()).in(values)));
        }
    }

    class BetweenPredicateBuilder implements PredicateBuilder {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            Object from = ValueResolver.resolve(filter.value(), field.type());
            Object to = ValueResolver.resolve(filter.valueTo(), field.type());
            return switch (field.type()) {
                case DATE -> {
                    Expression<Instant> dateKey = root.get(field.key());
                    yield cb.and(predicate, cb.between(dateKey, (Instant) from, (Instant) to));
                }
                case NUMBER -> {
                    Expression<Double> numKey = root.get(field.key());
                    yield cb.and(predicate, cb.between(numKey, ((Number) from).doubleValue(), ((Number) to).doubleValue()));
                }
                default -> {
                    Expression<Comparable<Object>> key = root.get(field.key());
                    yield cb.and(predicate, cb.between(key, (Comparable<Object>) from, (Comparable<Object>) to));
                }
            };
        }
    }

    class BlankPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            Predicate isNull = cb.isNull(root.get(field.key()));
            if (field.type() == FieldType.STRING) {
                Predicate isEmpty = cb.equal(root.get(field.key()), "");
                return cb.and(predicate, cb.or(isNull, isEmpty));
            }
            return cb.and(predicate, isNull);
        }
    }

    class NotBlankPredicateBuilder implements PredicateBuilder {
        @Override
        public <T> Predicate build(Root<T> root, CriteriaBuilder cb, Predicate predicate, SearchableField field, FilterRequest filter) {
            Predicate isNotNull = cb.isNotNull(root.get(field.key()));
            if (field.type() == FieldType.STRING) {
                Predicate isNotEmpty = cb.notEqual(root.get(field.key()), "");
                return cb.and(predicate, isNotNull, isNotEmpty);
            }
            return cb.and(predicate, isNotNull);
        }
    }

    // ─── Factory ────────────────────────────────────────────────────────

    private static Map<FilterOperator, PredicateBuilder> buildRegistry() {
        EnumMap<FilterOperator, PredicateBuilder> map = new EnumMap<>(FilterOperator.class);
        map.put(FilterOperator.EQUALS, new EqualsPredicateBuilder());
        map.put(FilterOperator.NOT_EQUALS, new NotEqualsPredicateBuilder());
        map.put(FilterOperator.LESS_THAN, new LessThanPredicateBuilder());
        map.put(FilterOperator.GREATER_THAN, new GreaterThanPredicateBuilder());
        map.put(FilterOperator.CONTAINS, new ContainsPredicateBuilder());
        map.put(FilterOperator.NOT_CONTAINS, new NotContainsPredicateBuilder());
        map.put(FilterOperator.STARTS_WITH, new StartsWithPredicateBuilder());
        map.put(FilterOperator.ENDS_WITH, new EndsWithPredicateBuilder());
        map.put(FilterOperator.IN, new InPredicateBuilder());
        map.put(FilterOperator.NOT_IN, new NotInPredicateBuilder());
        map.put(FilterOperator.BETWEEN, new BetweenPredicateBuilder());
        map.put(FilterOperator.BLANK, new BlankPredicateBuilder());
        map.put(FilterOperator.NOT_BLANK, new NotBlankPredicateBuilder());
        return Map.copyOf(map);
    }
}
