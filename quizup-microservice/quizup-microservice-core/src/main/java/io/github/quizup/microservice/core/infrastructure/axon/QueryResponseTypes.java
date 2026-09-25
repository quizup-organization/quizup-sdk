package io.github.quizup.microservice.core.infrastructure.axon;

import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.messaging.responsetypes.ResponseTypes;

import java.util.List;
import java.util.Optional;

/**
 * Point d'entrée unique et standardisé pour les {@link ResponseType} des queries QuizUp.
 *
 * <p>Les services ne doivent plus dépendre du {@code ResponseTypes} natif d'Axon : passer par
 * cette factory garantit un comportement homogène et corrige la limite de matching sur les
 * éléments génériques (voir {@link RawMultipleInstancesResponseType}).</p>
 */
public interface QueryResponseTypes {

    /** Réponse unique. */
    static <R> ResponseType<R> instanceOf(Class<R> type) {
        return ResponseTypes.instanceOf(type);
    }

    /** Réponse unique optionnelle. */
    static <R> ResponseType<Optional<R>> optionalInstanceOf(Class<R> type) {
        return ResponseTypes.optionalInstanceOf(type);
    }

    /** Liste de réponses (matche aussi les éléments génériques, ex. {@code List<Envelope<Event>>}). */
    static <R> ResponseType<List<R>> multipleInstancesOf(Class<R> type) {
        return new RawMultipleInstancesResponseType<>(type);
    }

    /** Résultat de recherche paginé (DTO de transport {@code SearchResponse}). */
    static <R> SearchResponseType<R> searchResponseOf(Class<R> type) {
        return new SearchResponseType<>(type);
    }
}
