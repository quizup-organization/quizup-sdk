package io.github.quizup.microservice.core.domain.exception;

public enum ProblemCategory {
    /**
     * Erreur technique (base de données, réseau, etc.)
     */
    TECHNICAL,

    /**
     * Problème de permissions/autorisation
     */
    PERMISSION,

    /**
     * Erreur de validation des données d'entrée
     */
    VALIDATION,

    /**
     * Erreur métier liée à l'état d'un agrégat
     */
    BUSINESS_AGGREGATE,

    /**
     * Commande invalide (validation métier)
     */
    BUSINESS_INVALID_COMMAND,

    /**
     * Ressource métier manquante (Query)
     */
    BUSINESS_RESOURCE_MISSING
}
