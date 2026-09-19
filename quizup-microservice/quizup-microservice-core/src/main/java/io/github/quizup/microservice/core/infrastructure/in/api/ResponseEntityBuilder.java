package io.github.quizup.microservice.core.infrastructure.in.api;

import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
import org.springframework.http.ResponseEntity;

import java.net.URI;

public final class ResponseEntityBuilder {
    private ResponseEntityBuilder() {
    }

    public static ResponseEntity<IdResponse> creation(String endpoint, String id) {
        return ResponseEntity
                .created(URI.create(String.format("%s/%s", endpoint, id)))
                .body(new IdResponse(id));
    }

    public static ResponseEntity<IdResponse> ok(String id) {
        return ResponseEntity.ok(new IdResponse(id));
    }
}
