package io.github.quizup.microservice.core.domain.model.search;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Critère de tri. Porte l'info de type Jackson ({@code @class}) pour la désérialisation des
 * queries de recherche sur le bus distribué (champ déclaré en interface).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface SortCriteria {
    String property();

    SortDirection direction();
}
