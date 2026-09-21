package io.github.quizup.microservice.autoconfigure;

import io.github.quizup.microservice.MicroserviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuration automatique pour CORS (Cross-Origin Resource Sharing).
 * <p>
 * Permet de configurer les requêtes cross-origin de manière flexible via les propriétés.
 * Activé par défaut, peut être désactivé avec: microservice.cors.enabled=false
 * <p>
 * Configuration par défaut:
 * - Autorise toutes les origines avec pattern "*"
 * - Autorise toutes les méthodes HTTP (GET, POST, PUT, DELETE, PATCH, OPTIONS)
 * - Autorise tous les headers
 * - Autorise les credentials (cookies, authorization headers)
 * - Cache la réponse pré-flight pendant 1 heure
 * <p>
 * Exemple de configuration personnalisée:
 * <pre>
 * microservice:
 *   cors:
 *     enabled: true
 *     allowed-origins:
 *       - http://localhost:3000
 *       - http://localhost:4200
 *     allowed-methods:
 *       - GET
 *       - POST
 *       - PUT
 *       - DELETE
 *     allowed-headers:
 *       - Content-Type
 *       - Authorization
 *     exposed-headers:
 *       - X-Total-Count
 *     allow-credentials: true
 *     max-age: 3600
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(CorsFilter.class)
@ConditionalOnProperty(prefix = "microservice.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CorsAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CorsAutoConfiguration.class);

    private final MicroserviceProperties properties;

    public CorsAutoConfiguration(MicroserviceProperties properties) {
        this.properties = properties;
        logger.info("CorsAutoConfiguration enabled");
    }

    @Bean
    public CorsFilter corsFilter() {
        MicroserviceProperties.Cors corsProps = properties.cors();

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Configuration des credentials
        config.setAllowCredentials(corsProps.allowCredentials());

        // Configuration des origines
        // Si allowCredentials est true et qu'on a "*", on utilise allowedOriginPattern
        // Sinon on utilise allowedOrigins
        if (corsProps.allowCredentials() && corsProps.allowedOrigins().contains("*")) {
            config.addAllowedOriginPattern("*");
            logger.info("CORS: Using origin pattern '*' (allowCredentials=true)");
        } else {
            corsProps.allowedOrigins().forEach(config::addAllowedOrigin);
            logger.info("CORS: Allowed origins: {}", corsProps.allowedOrigins());
        }

        // Configuration des méthodes HTTP
        corsProps.allowedMethods().forEach(config::addAllowedMethod);
        logger.debug("CORS: Allowed methods: {}", corsProps.allowedMethods());

        // Configuration des headers autorisés
        corsProps.allowedHeaders().forEach(config::addAllowedHeader);
        logger.debug("CORS: Allowed headers: {}", corsProps.allowedHeaders());

        // Configuration des headers exposés
        if (!corsProps.exposedHeaders().isEmpty()) {
            corsProps.exposedHeaders().forEach(config::addExposedHeader);
            logger.debug("CORS: Exposed headers: {}", corsProps.exposedHeaders());
        }

        // Configuration du cache pré-flight
        config.setMaxAge(corsProps.maxAge());
        logger.debug("CORS: Max age: {} seconds", corsProps.maxAge());

        source.registerCorsConfiguration("/**", config);

        logger.info("CORS configuration successfully applied - allowCredentials: {}",
                corsProps.allowCredentials());

        return new CorsFilter(source);
    }
}
