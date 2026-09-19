package io.github.quizup.microservice.core.domain.model.search;

public record DefaultPageCriteria(Integer size, Integer number) implements PageCriteria {
}
