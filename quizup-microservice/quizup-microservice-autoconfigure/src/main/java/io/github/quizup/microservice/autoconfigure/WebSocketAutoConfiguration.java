package io.github.quizup.microservice.autoconfigure;

import io.github.quizup.microservice.MicroserviceProperties;
import io.github.quizup.microservice.autoconfigure.websocket.StompAuthChannelInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Auto-configuration WebSocket STOMP partagée par tous les microservices.
 * <p>
 * Configure un message broker simple avec les destinations {@code /topic} et {@code /queue},
 * un préfixe applicatif {@code /app}, et un endpoint SockJS sur {@code /ws}.
 * <p>
 * Activée par défaut, peut être désactivée avec :
 * <pre>
 * microservice:
 *   websocket:
 *     enabled: false
 * </pre>
 * <p>
 * Personnalisation possible :
 * <pre>
 * microservice:
 *   websocket:
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
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(WebSocketMessageBrokerConfigurer.class)
@ConditionalOnProperty(prefix = "microservice.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocketMessageBroker
public class WebSocketAutoConfiguration implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAutoConfiguration.class);

    private final MicroserviceProperties.WebSocketProperties wsProperties;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;

    public WebSocketAutoConfiguration(MicroserviceProperties properties,
                                      ObjectProvider<JwtDecoder> jwtDecoderProvider) {
        this.wsProperties = properties.getWebsocket();
        this.jwtDecoderProvider = jwtDecoderProvider;
        logger.info("WebSocket auto-configuration enabled — endpoint: {}", wsProperties.getEndpoint());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                new StompAuthChannelInterceptor(jwtDecoderProvider, wsProperties.isRequireAuth()));
        logger.info("WebSocket STOMP authentication interceptor registered (require-auth: {})",
                wsProperties.isRequireAuth());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        String[] destinations = wsProperties.getBrokerDestinations().toArray(String[]::new);
        config.enableSimpleBroker(destinations);
        config.setApplicationDestinationPrefixes(wsProperties.getApplicationDestinationPrefix());
        logger.info("WebSocket broker destinations: {}, app prefix: {}",
                wsProperties.getBrokerDestinations(),
                wsProperties.getApplicationDestinationPrefix());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = wsProperties.getAllowedOriginPatterns().toArray(String[]::new);

        var endpoint = registry.addEndpoint(wsProperties.getEndpoint())
                .setAllowedOriginPatterns(origins);

        if (wsProperties.isWithSockJs()) {
            endpoint.withSockJS();
        }

        logger.info("WebSocket STOMP endpoint registered: {} (SockJS: {}, origins: {})",
                wsProperties.getEndpoint(),
                wsProperties.isWithSockJs(),
                wsProperties.getAllowedOriginPatterns());
    }
}
