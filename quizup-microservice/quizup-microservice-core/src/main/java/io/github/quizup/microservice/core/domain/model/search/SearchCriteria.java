package io.github.quizup.microservice.core.domain.model.search;

import java.util.Collections;
import java.util.List;

public interface SearchCriteria {
    List<FilterCriteria> filters();

    List<SortCriteria> sorts();

    PageCriteria page();

    static SearchCriteria empty() {
        return new DefaultSearchCriteria(
                Collections.emptyList(),
                Collections.emptyList(),
                PageCriteria.firstPage()
        );
    }
}
