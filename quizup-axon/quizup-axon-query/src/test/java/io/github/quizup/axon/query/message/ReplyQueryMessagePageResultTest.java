package io.github.quizup.axon.query.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.github.quizup.microservice.core.domain.model.search.DefaultPageResult;
import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.infrastructure.axon.PageResultResponseType;
import org.axonframework.queryhandling.GenericQueryResponseMessage;
import org.axonframework.queryhandling.QueryResponseMessage;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ReplyQueryMessagePageResultTest {

    record Row(String id, String name) {
    }

    @Test
    void pageResultElementsKeepTheirTypeThroughTransport() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule());
        Serializer serializer = JacksonSerializer.builder().objectMapper(objectMapper).build();
        PageResult<Row> page = new DefaultPageResult<>(
                List.of(new Row("a", "A"), new Row("b", "B")),
                0, 10, 2, 1, List.of(), true, true, false);
        PageResultResponseType<Row> responseType = new PageResultResponseType<>(Row.class);

        ReplyQueryMessage wire = new ReplyQueryMessage(
                "q1", new GenericQueryResponseMessage<>(page), serializer, responseType);

        QueryResponseMessage<?> response = wire.getQueryResponseMessage(serializer);
        Object payload = response.getPayload();

        PageResult<?> back = assertInstanceOf(PageResult.class, payload);
        assertEquals(2, back.content().size());
        Row first = assertInstanceOf(Row.class, back.content().get(0));
        assertEquals("a", first.id());
        assertEquals("A", first.name());
    }
}
