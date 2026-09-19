package io.github.quizup.microservice.core.domain.model.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque un champ d'entité JPA comme filtrable/triable dans les recherches dynamiques.
 *
 * - {@code type}  : type sémantique du champ, utilisé pour valider les opérateurs et convertir les valeurs.
 * - {@code alias} : nom public exposé dans les DTOs/responses et reçu dans les filtres client.
 *                   Si vide, le nom du champ Java est utilisé comme alias.
 *
 * Exemple :
 * <pre>
 *   @Searchable(type = FieldType.STRING, alias = "id")
 *   private String userId;
 * </pre>
 * Un client peut filtrer avec {@code property = "id"}, qui sera résolu vers le champ JPA {@code userId}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Searchable {

    /**
     * Type sémantique du champ.
     */
    FieldType type();

    /**
     * Nom public utilisé dans les filtres clients (alias du champ JPA).
     * Si non renseigné, le nom du champ Java est utilisé.
     */
    String alias() default "";
}

