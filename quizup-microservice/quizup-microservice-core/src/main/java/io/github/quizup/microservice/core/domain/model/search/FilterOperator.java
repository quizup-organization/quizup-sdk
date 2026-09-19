package io.github.quizup.microservice.core.domain.model.search;

/**
 * Opérateurs de filtrage disponibles dans le domaine
 */
public enum FilterOperator {
    /**
     * Inférieur à - Comparaison numérique ou de date
     */
    LESS_THAN,

    /**
     * Supérieur à - Comparaison numérique ou de date
     */
    GREATER_THAN,

    /**
     * Égal à - Comparaison exacte
     */
    EQUALS,

    /**
     * Différent de - Comparaison d'inégalité
     */
    NOT_EQUALS,

    /**
     * Contient - Recherche de sous-chaîne (case insensitive)
     */
    CONTAINS,

    /**
     * Ne contient pas - Inverse de CONTAINS
     */
    NOT_CONTAINS,

    /**
     * Dans la liste - Vérifie si la valeur est dans une liste de valeurs
     */
    IN,

    /**
     * Pas dans la liste - Inverse de IN
     */
    NOT_IN,

    /**
     * Entre deux valeurs - Nécessite value et valueTo
     */
    BETWEEN,

    /**
     * Commence par - Recherche de préfixe
     */
    STARTS_WITH,

    /**
     * Finit par - Recherche de suffixe
     */
    ENDS_WITH,

    /**
     * Champ vide ou null
     */
    BLANK,

    /**
     * Champ non vide et non null
     */
    NOT_BLANK
}
