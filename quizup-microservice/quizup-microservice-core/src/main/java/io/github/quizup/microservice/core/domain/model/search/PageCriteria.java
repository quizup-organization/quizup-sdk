package io.github.quizup.microservice.core.domain.model.search;

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
