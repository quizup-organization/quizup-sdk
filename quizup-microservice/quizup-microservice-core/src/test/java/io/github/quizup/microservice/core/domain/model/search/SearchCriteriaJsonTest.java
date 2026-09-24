package io.github.quizup.microservice.core.domain.model.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.microservice.core.infrastructure.in.api.request.PageRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SearchCriteriaJsonTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void restSearchRequestWithoutTypeId() throws Exception {
        SearchRequest req = om.readValue("{\"page\":{\"number\":0,\"size\":2}}", SearchRequest.class);
        assertNotNull(req.page());
        assertEquals(2, req.page().size());
    }

    @Test
    void busInterfaceTypedPageRoundTrips() throws Exception {
        DefaultSearchCriteria criteria =
                new DefaultSearchCriteria(List.of(), List.of(), new PageRequest(0, 2));
        String json = om.writeValueAsString(criteria);
        DefaultSearchCriteria back = om.readValue(json, DefaultSearchCriteria.class);
        assertInstanceOf(PageRequest.class, back.page());
        assertEquals(2, back.page().size());
        assertEquals(0, back.page().number());
    }
}
