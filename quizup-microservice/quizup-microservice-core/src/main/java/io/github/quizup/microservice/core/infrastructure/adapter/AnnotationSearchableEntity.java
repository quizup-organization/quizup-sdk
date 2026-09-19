package io.github.quizup.microservice.core.infrastructure.adapter;

import io.github.quizup.microservice.core.domain.exception.SearchableProblems;
import io.github.quizup.microservice.core.domain.model.search.FieldType;
import io.github.quizup.microservice.core.domain.model.search.Searchable;
import io.github.quizup.microservice.core.domain.model.search.SearchableEntity;
import io.github.quizup.microservice.core.domain.model.search.SearchableField;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Implémentation de {@link SearchableEntity} construite par réflexion à partir
 * des champs annotés {@link Searchable} d'une entité JPA.
 *
 * La liste des champs est construite une seule fois à l'instanciation et mise en cache.
 * Utilisation :
 * <pre>
 *   new AnnotationSearchableEntity(UserEntity.class)
 * </pre>
 */
public class AnnotationSearchableEntity implements SearchableEntity {

    private final List<SearchableField> fields;
    private final Class<?> entityClass;

    /**
     * @param entityClass la classe JPA à scanner (example : UserEntity.class)
     * @throws SearchableProblems.NoSearchableFieldsFoundProblem si aucun champ @Searchable n'est trouvé
     */
    public AnnotationSearchableEntity(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.fields = buildFields(entityClass);

        if (this.fields.isEmpty()) {
            throw new SearchableProblems.NoSearchableFieldsFoundProblem(entityClass);
        }
    }

    @Override
    public List<SearchableField> fields() {
        return fields;
    }

    /**
     * Scanne récursivement la hiérarchie de classes pour trouver tous les champs @Searchable,
     * y compris ceux hérités d'une superclasse (example : @MappedSuperclass).
     */
    private static List<SearchableField> buildFields(Class<?> clazz) {
        if (clazz == null || clazz.equals(Object.class)) {
            return List.of();
        }

        List<SearchableField> declared = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Searchable.class))
                .map(AnnotationSearchableEntity::toSearchableField)
                .toList();

        List<SearchableField> inherited = buildFields(clazz.getSuperclass());

        return java.util.stream.Stream.concat(declared.stream(), inherited.stream()).toList();
    }

    private static SearchableField toSearchableField(Field field) {
        Searchable annotation = field.getAnnotation(Searchable.class);
        String key = field.getName();
        String alias = annotation.alias().isBlank() ? key : annotation.alias();
        FieldType type = annotation.type();
        return new SearchableField(key, alias, type, false);
    }

    @Override
    public String toString() {
        return "AnnotationSearchableEntity{entity=" + entityClass.getSimpleName() +
               ", fields=" + fields + "}";
    }
}

