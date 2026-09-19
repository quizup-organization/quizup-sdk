package io.github.quizup.axon.test;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.test.matchers.Matchers;
import org.hamcrest.Matcher;

import java.util.function.Predicate;

/**
 * Facilité pour construire des matchers de liste d'event messages à partir d'un
 * {@link Predicate} sur le payload. S'appuie sur {@link Matchers} d'Axon
 * ({@code payloadsMatching} + {@code exactSequenceOf} + {@code andNoMore}).
 */
public final class QuizUpAxonMatchers {

    private QuizUpAxonMatchers() {
    }

    /**
     * Matche une liste d'event messages dont le payload unique (séquence exacte, rien de plus)
     * satisfait le {@code predicate}.
     */
    public static Matcher<? extends java.util.List<? super EventMessage<?>>> singlePayloadMatching(
            Class<?> payloadType, Predicate<Object> predicate) {
        return Matchers.payloadsMatching(
                Matchers.exactSequenceOf(
                        Matchers.matches(m -> payloadType.isInstance(m) && predicate.test(m)),
                        Matchers.andNoMore()
                )
        );
    }

    /**
     * Matche une liste d'event messages contenant (au moins) un payload de type {@code payloadType}
     * satisfaisant le {@code predicate}. La liste peut contenir d'autres événements.
     */
    public static Matcher<? extends java.util.List<? super EventMessage<?>>> hasPayloadMatching(
            Class<?> payloadType, Predicate<Object> predicate) {
        return Matchers.payloadsMatching(
                Matchers.listWithAllOf(
                        Matchers.matches(m -> payloadType.isInstance(m) && predicate.test(m))
                )
        );
    }
}
