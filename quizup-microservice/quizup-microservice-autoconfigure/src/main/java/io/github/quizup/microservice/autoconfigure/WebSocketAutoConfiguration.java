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
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.time.Duration;

/**
 * Auto-configuration WebSocket STOMP partagée par tous les microservices.
 * <p>
 * Configure un message broker simple avec les destinations {@code /topic} et {@code /queue},
 * un préfixe applicatif {@code /app}, et un endpoint SockJS sur {@code /ws}.
 * <p>
 * Le broker porte des <b>heartbeats STOMP</b> ({@code heartbeat-outgoing} /
 * {@code heartbeat-incoming}, 10 s par défaut) : un client silencieux (app suspendue, réseau
 * coupé, half-open) est fermé par le broker après {@code max(client, serveur) × 3}, ce qui
 * déclenche la déconnexion de session (et donc la présence hors ligne). {@code 0} désactive.
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
 *     heartbeat-outgoing: 10s
 *     heartbeat-incoming: 10s
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(WebSocketMessageBrokerConfigurer.class)
@ConditionalOnProperty(prefix = "microservice.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocketMessageBroker
public class WebSocketAutoConfiguration implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAutoConfiguration.class);

    private final MicroserviceProperties.WebSocket wsProperties;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final ObjectProvider<TaskScheduler> taskSchedulerProvider;

    public WebSocketAutoConfiguration(MicroserviceProperties properties,
                                      ObjectProvider<JwtDecoder> jwtDecoderProvider,
                                      ObjectProvider<TaskScheduler> taskSchedulerProvider) {
        this.wsProperties = properties.websocket();
        this.jwtDecoderProvider = jwtDecoderProvider;
        this.taskSchedulerProvider = taskSchedulerProvider;
        logger.info("WebSocket auto-configuration enabled — endpoint: {}", wsProperties.endpoint());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                new StompAuthChannelInterceptor(jwtDecoderProvider, wsProperties.requireAuth()));
        logger.info("WebSocket STOMP authentication interceptor registered (require-auth: {})",
                wsProperties.requireAuth());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        String[] destinations = wsProperties.brokerDestinations().toArray(String[]::new);
        var broker = config.enableSimpleBroker(destinations);
        configureHeartbeats(broker);
        config.setApplicationDestinationPrefixes(wsProperties.applicationDestinationPrefix());
        logger.info("WebSocket broker destinations: {}, app prefix: {}",
                wsProperties.brokerDestinations(),
                wsProperties.applicationDestinationPrefix());
    }

    private void configureHeartbeats(SimpleBrokerRegistration broker) {
        long outgoing = Math.max(0, wsProperties.heartbeatOutgoing().toMillis());
        long incoming = Math.max(0, wsProperties.heartbeatIncoming().toMillis());
        if (outgoing == 0 && incoming == 0) {
            logger.info("WebSocket STOMP heartbeats disabled");
            return;
        }
        TaskScheduler scheduler = taskSchedulerProvider.orderedStream().findFirst().orElse(null);
        if (scheduler == null) {
            logger.warn("WebSocket STOMP heartbeats configured but no TaskScheduler is available");
            return;
        }
        broker.setTaskScheduler(scheduler);
        broker.setHeartbeatValue(new long[] { outgoing, incoming });
        logger.info("WebSocket STOMP heartbeats enabled: outgoing={}ms, incoming={}ms",
                outgoing, incoming);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = wsProperties.allowedOriginPatterns().toArray(String[]::new);

        var endpoint = registry.addEndpoint(wsProperties.endpoint())
                .setAllowedOriginPatterns(origins);

        if (wsProperties.withSockJs()) {
            endpoint.withSockJS();
        }

        logger.info("WebSocket STOMP endpoint registered: {} (SockJS: {}, origins: {})",
                wsProperties.endpoint(),
                wsProperties.withSockJs(),
                wsProperties.allowedOriginPatterns());
    }
}
