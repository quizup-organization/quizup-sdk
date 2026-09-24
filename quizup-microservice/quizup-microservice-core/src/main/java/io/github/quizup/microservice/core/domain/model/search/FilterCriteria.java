package io.github.quizup.microservice.core.domain.model.search;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * Critère de filtrage. Porte l'info de type Jackson ({@code @class}) pour la désérialisation
 * des queries de recherche sur le bus distribué (champ déclaré en interface).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface FilterCriteria {
    String property();
    FilterOperator operator();
    Object value();
    Object valueTo();
    List<Object> values();
}
