package io.github.quizup.microservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for QuizUp Microservices Starter.
 * <p>
 * Provides auto-configuration for CORS, Swagger/OpenAPI, Exception Handling, and Actuator.
 * <p>
 * Example configuration:
 * <pre>
 * microservice:
 *   cors:
 *     enabled: true
 *     allowed-origins: http://localhost:3000,http://localhost:4200
 *     allowed-methods: GET,POST,PUT,DELETE
 *     allowed-headers: "*"
 *     allow-credentials: true
 *     max-age: 3600
 *   swagger:
 *     enabled: true
 *     version: 1.0.0
 *     description: API Documentation
 *     contact:
 *       name: QuizUp Team
 *       email: contact@quizup.com
 *       url: https://quizup.com
 *     license:
 *       name: Apache 2.0
 *       url: https://www.apache.org/licenses/LICENSE-2.0.html
 *   exception-handler:
 *     enabled: true
 *     log-stack-trace: true
 *     include-binding-errors: true
 *   actuator:
 *     enabled: true
 * </pre>
 */
@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "microservice")
public class MicroserviceProperties {

    @Valid
    @NestedConfigurationProperty
    private CorsProperties cors = new CorsProperties();

    @Valid
    @NestedConfigurationProperty
    private SwaggerProperties swagger = new SwaggerProperties();

    @Valid
    @NestedConfigurationProperty
    private ExceptionHandlerProperties exceptionHandler = new ExceptionHandlerProperties();

    @Valid
    @NestedConfigurationProperty
    private ActuatorProperties actuator = new ActuatorProperties();

    @Valid
    @NestedConfigurationProperty
    private ResourceServerProperties resourceServer = new ResourceServerProperties();

    @Valid
    @NestedConfigurationProperty
    private WebSocketProperties websocket = new WebSocketProperties();

    /**
     * Configuration properties for CORS (Cross-Origin Resource Sharing)
     */
    @Setter
    @Getter
    public static class CorsProperties {

        /**
         * Enable or disable CORS configuration
         */
        private boolean enabled = true;

        /**
         * List of allowed origins. Use "*" for all origins or specific URLs.
         * When allowCredentials is true, cannot use "*" - must specify actual origins.
         */
        @NotEmpty(message = "At least one allowed origin must be specified")
        private List<String> allowedOrigins = List.of("*");

        /**
         * List of allowed HTTP methods
         */
        @NotEmpty(message = "At least one allowed method must be specified")
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

        /**
         * List of allowed headers
         */
        @NotEmpty(message = "At least one allowed header must be specified")
        private List<String> allowedHeaders = List.of("*");

        /**
         * List of exposed headers
         */
        private List<String> exposedHeaders = List.of();

        /**
         * Whether credentials (cookies, authorization headers) are allowed
         */
        private boolean allowCredentials = true;

        /**
         * How long (in seconds) the response from a pre-flight request can be cached
         */
        private Long maxAge = 3600L;

    }

    /**
     * Configuration properties for Swagger/OpenAPI documentation
     */
    @Setter
    @Getter
    public static class SwaggerProperties {

        /**
         * Enable or disable Swagger/OpenAPI configuration
         */
        private boolean enabled = true;

        /**
         * API version
         */
        @NotEmpty(message = "API version must be specified")
        private String version = "1.0.0";

        /**
         * API description
         */
        @NotEmpty(message = "API description must be specified")
        private String description = "API Documentation";

        /**
         * Terms of service URL
         */
        private String termsOfService;

        private boolean useRootPath = true;

        private boolean showOauth2Endpoints = false;

        /**
         * Contact information
         */
        @Valid
        @NestedConfigurationProperty
        private ContactProperties contact = new ContactProperties();

        /**
         * License information
         */
        @Valid
        @NestedConfigurationProperty
        private LicenseProperties license = new LicenseProperties();

        /**
         * OAuth2 configuration for Swagger UI
         */
        @Valid
        @NestedConfigurationProperty
        private OAuth2Properties oauth2 = new OAuth2Properties();

        /**
         * OAuth2 configuration for Swagger UI authentication
         */
        @Setter
        @Getter
        public static class OAuth2Properties {

            /**
             * Enable OAuth2 security in Swagger UI
             */
            private boolean enabled = true;

            /**
             * Authorization Server URL (issuer)
             */
            private String authorizationServerUrl = "http://localhost:8085";

            /**
             * OAuth2 Client ID for Swagger UI
             */
            private String clientId = "swagger";

            /**
             * OAuth2 scopes to request
             */
            private List<String> scopes = List.of("openid", "profile");

            /**
             * Use PKCE (Proof Key for Code Exchange) - recommended for public clients
             */
            private boolean usePkce = true;

        }

        /**
         * Contact information for the API
         */
        @Setter
        @Getter
        public static class ContactProperties {

            /**
             * Contact name
             */
            private String name = "QuizUp Team";

            /**
             * Contact email
             */
            private String email = "contact@quizup.com";

            /**
             * Contact URL
             */
            private String url;

        }

        /**
         * License information for the API
         */
        @Setter
        @Getter
        public static class LicenseProperties {

            /**
             * License name
             */
            private String name = "Apache 2.0";

            /**
             * License URL
             */
            private String url = "https://www.apache.org/licenses/LICENSE-2.0.html";

        }
    }

    /**
     * Configuration properties for Exception Handler
     */
    @Setter
    @Getter
    public static class ExceptionHandlerProperties {

        /**
         * Enable or disable global exception handler
         */
        private boolean enabled = true;

        /**
         * Log full stack trace for errors
         */
        private boolean logStackTrace = true;

        /**
         * Include binding errors in validation exception responses
         */
        private boolean includeBindingErrors = true;

    }

    /**
     * Configuration properties for Spring Boot Actuator
     */
    @Setter
    @Getter
    public static class ActuatorProperties {

        /**
         * Enable or disable Actuator auto-configuration
         */
        private boolean enabled = true;

    }

    /**
     * Configuration properties for Resource Server (OAuth2 JWT validation)
     */
    @Setter
    @Getter
    public static class ResourceServerProperties {

        /**
         * Enable or disable Resource Server auto-configuration
         */
        private boolean enabled = true;

        /**
         * JWT configuration
         */
        @Valid
        @NestedConfigurationProperty
        private JwtProperties jwt = new JwtProperties();

        @Setter
        @Getter
        public static class JwtProperties {

            /**
             * JWT Issuer URI (for token validation)
             */
            private String issuerUri = "http://localhost:8085";

            /**
             * JWK Set URI (for fetching public keys)
             */
            private String jwkSetUri = "http://localhost:8085/oauth2/jwks";

        }

    }

    /**
     * Configuration properties for WebSocket STOMP.
     * <p>
     * Example:
     * <pre>
     * microservice:
     *   websocket:
     *     enabled: true
     *     endpoint: /ws
     *     application-destination-prefix: /app
     *     broker-destinations:
     *       - /topic
     *       - /queue
     *     allowed-origin-patterns:
     *       - "*"
     *     with-sock-js: true
     * </pre>
     */
    @Setter
    @Getter
    public static class WebSocketProperties {

        /**
         * Enable or disable WebSocket auto-configuration
         */
        private boolean enabled = true;

        /**
         * STOMP endpoint path (SockJS handshake)
         */
        @NotEmpty(message = "WebSocket endpoint must be specified")
        private String endpoint = "/ws";

        /**
         * Application destination prefix for @MessageMapping methods
         */
        @NotEmpty(message = "Application destination prefix must be specified")
        private String applicationDestinationPrefix = "/app";

        /**
         * Simple broker destination prefixes
         */
        @NotEmpty(message = "At least one broker destination must be specified")
        private List<String> brokerDestinations = List.of("/topic", "/queue");

        /**
         * Allowed origin patterns for the WebSocket endpoint
         */
        @NotEmpty(message = "At least one allowed origin pattern must be specified")
        private List<String> allowedOriginPatterns = List.of("*");

        /**
         * Whether to enable SockJS fallback
         */
        private boolean withSockJs = true;

        /**
         * Whether STOMP CONNECT frames must carry a valid JWT. When {@code false}
         * (default), unauthenticated sessions are still accepted as anonymous.
         */
        private boolean requireAuth = false;

    }
}
