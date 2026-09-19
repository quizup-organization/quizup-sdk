package io.github.quizup.microservice.core.domain.model.search;

/**
 * Descripteur d'un champ filtrable/triable.
 *
 * - {@code key}   : nom réel du champ Java dans l'entité JPA → utilisé dans le prédicat JPA.
 * - {@code alias} : nom public reçu du client dans les filtres → résolu vers {@code key}.
 * - {@code type}  : type sémantique pour la validation et la conversion des valeurs.
 */
public record SearchableField(
        String key,
        String alias,
        FieldType type,
        boolean nullable
) {
    /**
     * Constructeur sans alias : l'alias est identique à la clé JPA.
     */
    public SearchableField(String key, FieldType type, boolean nullable) {
        this(key, key, type, nullable);
    }

    /**
     * Constructeur minimal : alias = key, nullable = false.
     */
    public SearchableField(String key, FieldType type) {
        this(key, key, type, false);
    }

    /**
     * Constructeur avec alias explicite, nullable = false.
     */
    public SearchableField(String key, String alias, FieldType type) {
        this(key, alias, type, false);
    }

    /**
     * Vérifie si ce champ correspond à la clé reçue (par key ou alias).
     */
    public boolean matches(String input) {
        return key.equals(input) || alias.equals(input);
    }
}
