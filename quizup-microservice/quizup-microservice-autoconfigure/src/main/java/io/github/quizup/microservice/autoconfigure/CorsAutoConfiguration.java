package io.github.quizup.microservice.autoconfigure;

import io.github.quizup.microservice.MicroserviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(MicroserviceProperties.class)
public class CorsAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CorsAutoConfiguration.class);

    private final MicroserviceProperties properties;

    public CorsAutoConfiguration(MicroserviceProperties properties) {
        this.properties = properties;
        logger.info("CorsAutoConfiguration enabled");
    }

    @Bean
    public CorsFilter corsFilter() {
        MicroserviceProperties.CorsProperties corsProps = properties.getCors();

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Configuration des credentials
        config.setAllowCredentials(corsProps.isAllowCredentials());

        // Configuration des origines
        // Si allowCredentials est true et qu'on a "*", on utilise allowedOriginPattern
        // Sinon on utilise allowedOrigins
        if (corsProps.isAllowCredentials() && corsProps.getAllowedOrigins().contains("*")) {
            config.addAllowedOriginPattern("*");
            logger.info("CORS: Using origin pattern '*' (allowCredentials=true)");
        } else {
            corsProps.getAllowedOrigins().forEach(config::addAllowedOrigin);
            logger.info("CORS: Allowed origins: {}", corsProps.getAllowedOrigins());
        }

        // Configuration des méthodes HTTP
        corsProps.getAllowedMethods().forEach(config::addAllowedMethod);
        logger.debug("CORS: Allowed methods: {}", corsProps.getAllowedMethods());

        // Configuration des headers autorisés
        corsProps.getAllowedHeaders().forEach(config::addAllowedHeader);
        logger.debug("CORS: Allowed headers: {}", corsProps.getAllowedHeaders());

        // Configuration des headers exposés
        if (!corsProps.getExposedHeaders().isEmpty()) {
            corsProps.getExposedHeaders().forEach(config::addExposedHeader);
            logger.debug("CORS: Exposed headers: {}", corsProps.getExposedHeaders());
        }

        // Configuration du cache pré-flight
        config.setMaxAge(corsProps.getMaxAge());
        logger.debug("CORS: Max age: {} seconds", corsProps.getMaxAge());

        source.registerCorsConfiguration("/**", config);

        logger.info("CORS configuration successfully applied - allowCredentials: {}",
                corsProps.isAllowCredentials());

        return new CorsFilter(source);
    }
}
