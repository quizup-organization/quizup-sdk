package io.github.quizup.microservice.core.infrastructure.in.api.request;

import java.io.Serializable;

/**
 * DTO de recherche — critères de pagination (transport REST **et** bus Axon).
 */
public record PageRequest(
        Integer number,
        Integer size
) implements Serializable {
}
