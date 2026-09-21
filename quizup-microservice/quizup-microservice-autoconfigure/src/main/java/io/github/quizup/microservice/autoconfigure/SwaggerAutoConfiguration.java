package io.github.quizup.microservice.autoconfigure;

import io.github.quizup.microservice.MicroserviceProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.List;


/**
 * Configuration automatique pour Swagger/OpenAPI avec support OAuth2.
 * <p>
 * Configure automatiquement Swagger UI pour la documentation d'API.
 * Activé par défaut, peut être désactivé avec: microservice.swagger.enabled=false
 * <p>
 * La configuration par défaut fournit:
 * - Version de l'API: 1.0.0
 * - Description: API Documentation
 * - Contact: QuizUp Team (contact@quizup.com)
 * - License: Apache 2.0
 * <p>
 * Configuration OAuth2 (optionnelle):
 * <pre>
 * microservice:
 *   swagger:
 *     enabled: true
 *     oauth2:
 *       enabled: true
 *       authorization-server-url: http://localhost:8085
 *       client-id: swagger
 *       scopes:
 *         - openid
 *         - profile
 *       use-pkce: true
 * </pre>
 * <p>
 * La documentation Swagger est accessible par défaut à:
 * - Swagger UI: /swagger-ui.html
 * - API Docs (JSON): /v3/api-docs
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "microservice.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerAutoConfiguration.class);
    private static final String OAUTH2_SECURITY_SCHEME = "oauth2";

    private final MicroserviceProperties properties;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;
    private final SwaggerUiOAuthProperties swaggerUiOAuthProperties;

    private final SpringDocConfigProperties springDocConfigProperties;

    @Value("${spring.application.name:Service}")
    private String applicationName;

    public SwaggerAutoConfiguration(MicroserviceProperties properties,
                                    SwaggerUiConfigProperties swaggerUiConfigProperties,
                                    SwaggerUiOAuthProperties swaggerUiOAuthProperties, SpringDocConfigProperties springDocConfigProperties) {
        this.properties = properties;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        this.swaggerUiOAuthProperties = swaggerUiOAuthProperties;
        this.springDocConfigProperties = springDocConfigProperties;
        logger.info("SwaggerAutoConfiguration enabled");
    }

    @Bean
    public OpenAPI customOpenAPI() {
        MicroserviceProperties.Swagger swaggerProps = properties.swagger();
        MicroserviceProperties.Swagger.Contact contactProps = swaggerProps.contact();
        MicroserviceProperties.Swagger.License licenseProps = swaggerProps.license();
        MicroserviceProperties.Swagger.OAuth2 oauth2Props = swaggerProps.oauth2();

        logger.info("Configuring Swagger/OpenAPI for '{}' - version: {}", applicationName, swaggerProps.version());

        // Construction de l'objet Info
        Info info = new Info()
                .title(applicationName + " API")
                .version(swaggerProps.version())
                .description(swaggerProps.description());

        // Ajout des termes de service si spécifiés
        if (StringUtils.hasText(swaggerProps.termsOfService())) {
            info.termsOfService(swaggerProps.termsOfService());
            logger.debug("Swagger: Terms of service configured: {}", swaggerProps.termsOfService());
        }

        // Configuration du contact
        Contact contact = new Contact();

        if (StringUtils.hasText(contactProps.name())) {
            contact.name(contactProps.name());
        }

        if (StringUtils.hasText(contactProps.email())) {
            contact.email(contactProps.email());
        }

        if (StringUtils.hasText(contactProps.url())) {
            contact.url(contactProps.url());
        }

        info.contact(contact);

        logger.debug("Swagger: Contact configured - name: {}, email: {}", contactProps.name(), contactProps.email());

        // Configuration de la licence
        License license = new License();
        if (StringUtils.hasText(licenseProps.name())) {
            license.name(licenseProps.name());
        }

        if (StringUtils.hasText(licenseProps.url())) {
            license.url(licenseProps.url());
        }

        info.license(license);

        logger.debug("Swagger: License configured - name: {}", licenseProps.name());

        // Construire l'objet OpenAPI
        OpenAPI openAPI = new OpenAPI().info(info);

        // URL publique explicite du serveur (utile derrière un gateway) : sans cela, springdoc
        // déduit l'URL de la requête et génère l'hôte in-cluster (inaccessible du navigateur).
        if (StringUtils.hasText(swaggerProps.serverUrl())) {
            openAPI.setServers(List.of(new Server().url(swaggerProps.serverUrl())));
            logger.info("Swagger: server URL configured: {}", swaggerProps.serverUrl());
        }

        // Configuration OAuth2 si activée
        if (oauth2Props.enabled()) {
            configureOAuth2Security(openAPI, oauth2Props);
        }

        if (swaggerProps.showOauth2Endpoints()) {
            springDocConfigProperties.setShowOauth2Endpoints(true);
        }

        if (swaggerProps.useRootPath()) {
            swaggerUiConfigProperties.setUseRootPath(true);
        }

        logger.info("Swagger/OpenAPI configuration successfully applied - UI available at /swagger-ui.html");


        return openAPI;
    }

    /**
     * Configure la sécurité OAuth2 pour Swagger UI avec PKCE
     */
    private void configureOAuth2Security(OpenAPI openAPI, MicroserviceProperties.Swagger.OAuth2 oauth2Props) {
        String authServerUrl = oauth2Props.authorizationServerUrl();
        String authorizationUrl = authServerUrl + "/oauth2/authorize";
        String tokenUrl = authServerUrl + "/oauth2/token";

        logger.info("Configuring Swagger OAuth2 security with authorization server: {}", authServerUrl);
        logger.debug("OAuth2 Authorization URL: {}", authorizationUrl);
        logger.debug("OAuth2 Token URL: {}", tokenUrl);
        logger.debug("OAuth2 Client ID: {}", oauth2Props.clientId());
        logger.debug("OAuth2 PKCE enabled: {}", oauth2Props.usePkce());

        // Créer les scopes
        Scopes scopes = new Scopes();

        for (String scope : oauth2Props.scopes()) {
            scopes.addString(scope, "Scope: " + scope);
        }

        // L'extension x-usePkce est supprimée : elle n'est pas lue par Swagger UI
        OAuthFlow authorizationCodeFlow = new OAuthFlow()
                .authorizationUrl(authorizationUrl)
                .tokenUrl(tokenUrl)
                .scopes(scopes);

        OAuthFlows oAuthFlows = new OAuthFlows()
                .authorizationCode(authorizationCodeFlow);

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("OAuth2 Authorization Code" + (oauth2Props.usePkce() ? " with PKCE" : ""))
                .flows(oAuthFlows);

        openAPI.components(new Components()
                .addSecuritySchemes(OAUTH2_SECURITY_SCHEME, securityScheme));

        openAPI.addSecurityItem(new SecurityRequirement()
                .addList(OAUTH2_SECURITY_SCHEME, oauth2Props.scopes()));

        // ✅ Activation PKCE via les init-params Swagger UI
        if (oauth2Props.usePkce()) {
            swaggerUiOAuthProperties.setUsePkceWithAuthorizationCodeGrant(true);
            logger.info("Swagger UI PKCE enabled (usePkceWithAuthorizationCodeGrant=true)");
        }

        // Pré-remplissage du client_id dans la modale Swagger UI
        if (StringUtils.hasText(oauth2Props.clientId())) {
            swaggerUiOAuthProperties.setClientId(oauth2Props.clientId());
        }

        swaggerUiOAuthProperties.setScopes(oauth2Props.scopes());

        logger.info("Swagger OAuth2 security configured — auth: {}", authorizationUrl);
    }
}
