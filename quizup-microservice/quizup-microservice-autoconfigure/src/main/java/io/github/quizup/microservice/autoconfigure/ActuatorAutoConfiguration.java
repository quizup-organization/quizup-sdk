package io.github.quizup.microservice.autoconfigure;

import io.github.quizup.microservice.MicroserviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Configuration automatique pour Spring Boot Actuator.
 * <p>
 * Configure les endpoints Actuator pour le monitoring et la santé de l'application.
 * <p>
 * Cette configuration:
 * - Active les endpoints de monitoring (health, info, metrics)
 * - Résout les conflits entre Swagger/SpringDoc et Actuator
 * - Configure le mapping des endpoints web
 * <p>
 * Activé par défaut, peut être désactivé avec: microservice.actuator.enabled=false
 * <p>
 * Exemple de configuration:
 * <pre>
 * microservice:
 *   actuator:
 *     enabled: true
 *
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health,info,metrics
 *       base-path: /actuator
 *   endpoint:
 *     health:
 *       show-details: when-authorized
 * </pre>
 * <p>
 * Les endpoints Actuator sont accessibles par défaut à:
 * - Health: /actuator/health
 * - Info: /actuator/info
 * - Metrics: /actuator/metrics
 */
@AutoConfiguration
@ConditionalOnClass(WebMvcEndpointHandlerMapping.class)
@ConditionalOnProperty(prefix = "microservice.actuator", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MicroserviceProperties.class)
public class ActuatorAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ActuatorAutoConfiguration.class);

    public ActuatorAutoConfiguration() {
        logger.info("Actuator auto-configuration enabled");
    }

    /**
     * Configuration pour permettre à Swagger de fonctionner avec Actuator
     * Résout le conflit entre SpringDoc et Spring Boot Actuator
     */
    @Bean
    @ConditionalOnMissingBean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(
            WebEndpointsSupplier webEndpointsSupplier,
            ServletEndpointsSupplier servletEndpointsSupplier,
            ControllerEndpointsSupplier controllerEndpointsSupplier,
            EndpointMediaTypes endpointMediaTypes,
            CorsEndpointProperties corsProperties,
            WebEndpointProperties webEndpointProperties,
            Environment environment) {

        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());

        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);

        boolean shouldRegisterLinksMapping = shouldRegisterLinksMapping(
                webEndpointProperties, environment, basePath);

        return new WebMvcEndpointHandlerMapping(
                endpointMapping,
                webEndpoints,
                endpointMediaTypes,
                corsProperties.toCorsConfiguration(),
                new EndpointLinksResolver(allEndpoints, basePath),
                shouldRegisterLinksMapping);
    }

    private boolean shouldRegisterLinksMapping(
            WebEndpointProperties webEndpointProperties,
            Environment environment,
            String basePath) {

        return webEndpointProperties.getDiscovery().isEnabled() &&
                (StringUtils.hasText(basePath) ||
                        ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
    }
}
