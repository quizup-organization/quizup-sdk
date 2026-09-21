package io.github.quizup.microservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for the QuizUp Microservices Starter.
 * <p>
 * Records immuables : chaque composant a une valeur par défaut ({@link DefaultValue}), donc
 * les services ne déclarent que les propriétés qu'ils surchargent.
 */
@Validated
@ConfigurationProperties(prefix = "microservice")
public record MicroserviceProperties(
        @Valid @DefaultValue Cors cors,
        @Valid @DefaultValue Swagger swagger,
        @Valid @DefaultValue ExceptionHandler exceptionHandler,
        @Valid @DefaultValue Actuator actuator,
        @Valid @DefaultValue ResourceServer resourceServer,
        @Valid @DefaultValue WebSocket websocket) {

    /**
     * Configuration properties for CORS (Cross-Origin Resource Sharing).
     */
    public record Cors(
            @DefaultValue("true") boolean enabled,
            @NotEmpty @DefaultValue("*") List<String> allowedOrigins,
            @NotEmpty @DefaultValue({ "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS" }) List<String> allowedMethods,
            @NotEmpty @DefaultValue("*") List<String> allowedHeaders,
            @DefaultValue List<String> exposedHeaders,
            @DefaultValue("true") boolean allowCredentials,
            @DefaultValue("3600") Long maxAge) {
    }

    /**
     * Configuration properties for Swagger/OpenAPI documentation.
     */
    public record Swagger(
            @DefaultValue("true") boolean enabled,
            @NotEmpty @DefaultValue("1.0.0") String version,
            @NotEmpty @DefaultValue("API Documentation") String description,
            @DefaultValue("") String termsOfService,
            /**
             * URL publique du serveur OpenAPI (ex. derrière un gateway :
             * {@code https://api.example.com/<service>}). Si vide, springdoc déduit l'URL de la
             * requête (ce qui donne l'URL in-cluster lorsque la requête vient du gateway).
             */
            @DefaultValue("") String serverUrl,
            @DefaultValue("true") boolean useRootPath,
            @DefaultValue("false") boolean showOauth2Endpoints,
            @Valid @DefaultValue Contact contact,
            @Valid @DefaultValue License license,
            @Valid @DefaultValue OAuth2 oauth2) {

        /**
         * OAuth2 configuration for Swagger UI authentication.
         */
        public record OAuth2(
                @DefaultValue("true") boolean enabled,
                @DefaultValue("http://localhost:8085") String authorizationServerUrl,
                @DefaultValue("swagger") String clientId,
                @DefaultValue({ "openid", "profile" }) List<String> scopes,
                @DefaultValue("true") boolean usePkce) {
        }

        /**
         * Contact information for the API.
         */
        public record Contact(
                @DefaultValue("QuizUp Team") String name,
                @DefaultValue("contact@quizup.com") String email,
                @DefaultValue("") String url) {
        }

        /**
         * License information for the API.
         */
        public record License(
                @DefaultValue("Apache 2.0") String name,
                @DefaultValue("https://www.apache.org/licenses/LICENSE-2.0.html") String url) {
        }
    }

    /**
     * Configuration properties for the global exception handler.
     */
    public record ExceptionHandler(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("true") boolean logStackTrace,
            @DefaultValue("true") boolean includeBindingErrors) {
    }

    /**
     * Configuration properties for Spring Boot Actuator.
     */
    public record Actuator(
            @DefaultValue("true") boolean enabled) {
    }

    /**
     * Configuration properties for the Resource Server (OAuth2 JWT validation).
     */
    public record ResourceServer(
            @DefaultValue("true") boolean enabled,
            @Valid @DefaultValue Jwt jwt) {

        public record Jwt(
                @DefaultValue("http://localhost:8085") String issuerUri,
                @DefaultValue("http://localhost:8085/oauth2/jwks") String jwkSetUri) {
        }
    }

    /**
     * Configuration properties for WebSocket STOMP.
     */
    public record WebSocket(
            @DefaultValue("true") boolean enabled,
            @NotEmpty @DefaultValue("/ws") String endpoint,
            @NotEmpty @DefaultValue("/app") String applicationDestinationPrefix,
            @NotEmpty @DefaultValue({ "/topic", "/queue" }) List<String> brokerDestinations,
            @NotEmpty @DefaultValue("*") List<String> allowedOriginPatterns,
            @DefaultValue("true") boolean withSockJs,
            /**
             * Whether STOMP CONNECT frames must carry a valid JWT. When {@code false}
             * (default), unauthenticated sessions are still accepted as anonymous.
             */
            @DefaultValue("false") boolean requireAuth) {
    }
}
