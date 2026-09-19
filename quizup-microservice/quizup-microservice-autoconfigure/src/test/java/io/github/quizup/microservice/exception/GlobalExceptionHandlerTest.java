package io.github.quizup.microservice.exception;

import io.github.quizup.microservice.MicroserviceProperties;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;
import io.github.quizup.microservice.core.infrastructure.in.api.response.ExceptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(new MicroserviceProperties());
    }

    @Test
    void noResourceFoundExceptionShouldReturn404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/favicon.ico");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/favicon.ico");

        ResponseEntity<ExceptionResponse> response =
                handler.handleNoResourceFoundException(ex, new ServletWebRequest(request));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().type()).isEqualTo("not-found");
        assertThat(response.getBody().category()).isEqualTo(ProblemCategory.BUSINESS_RESOURCE_MISSING);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().path()).isEqualTo("/favicon.ico");
    }

    @Test
    void noResourceFoundExceptionShouldNotReturn500() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/images/missing.png");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/images/missing.png");

        ResponseEntity<ExceptionResponse> response =
                handler.handleNoResourceFoundException(ex, new ServletWebRequest(request));

        assertThat(response.getStatusCode().value()).isNotEqualTo(500);
    }

    @Test
    void catchAllExceptionShouldStillReturn500() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boom");

        ResponseEntity<ExceptionResponse> response =
                handler.handleGlobalException(new RuntimeException("boom"), new ServletWebRequest(request));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().type()).isEqualTo("internal-server-error");
    }
}
