package io.github.quizup.microservice.core.domain.model.search;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Critères de pagination. Porte l'info de type Jackson ({@code @class}) : les queries de recherche
 * transitent sérialisées sur le bus distribué, et ce champ est déclaré en interface — sans
 * discriminateur, la désérialisation échoue côté service distant.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface PageCriteria {

    Integer size();

    Integer number();

    static PageCriteria unpaged() {
        return new DefaultPageCriteria(0, 0);
    }

    static PageCriteria firstPage() {
        return new DefaultPageCriteria(20, 0);
    }
}
