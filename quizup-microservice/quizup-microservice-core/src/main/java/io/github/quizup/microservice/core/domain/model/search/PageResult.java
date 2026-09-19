package io.github.quizup.microservice.core.domain.model.search;

import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public interface PageResult<T> {

    @NonNull
    List<T> content();

    int pageNumber();

    int pageSize();

    long totalElements();

    int totalPages();

    List<SortCriteria> sorts();

    boolean first();

    boolean last();

    boolean empty();

    default <O> PageResult<O> map(Function<T, O> mapper) {
        return of(this, mapper);
    }

    static <T> PageResult<T> unpaged() {
        return new DefaultPageResult<>(
                Collections.emptyList(),
                0,
                0,
                0,
                0,
                Collections.emptyList(),
                true,
                true,
                true
        );
    }

    static <I, O> PageResult<O> of(PageResult<I> pageResult, Function<I, O> mapper) {
        List<O> users = pageResult.content().stream()
                .map(mapper)
                .toList();

        return new DefaultPageResult<>(
                users,
                pageResult.pageNumber(),
                pageResult.pageSize(),
                pageResult.totalElements(),
                pageResult.totalPages(),
                pageResult.sorts(),
                pageResult.first(),
                pageResult.last(),
                pageResult.empty()
        );
    }
}
