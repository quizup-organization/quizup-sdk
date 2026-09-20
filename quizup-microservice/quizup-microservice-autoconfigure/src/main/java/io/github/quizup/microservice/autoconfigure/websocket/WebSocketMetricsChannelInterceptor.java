package io.github.quizup.microservice.autoconfigure.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * Comptabilise les trames STOMP traitées par les canaux inbound/outbound du broker simple.
 * <p>
 * Une seule métrique {@code quizup.websocket.messages} avec les tags {@code direction}
 * ({@code inbound}/{@code outbound}), {@code command} (CONNECT, SUBSCRIBE, SEND, MESSAGE,
 * DISCONNECT…) et {@code result} ({@code sent}/{@code failed}). L'erreur de livraison est
 * détectée dans {@link #afterSendCompletion}.
 */
public class WebSocketMetricsChannelInterceptor implements ChannelInterceptor {

    private static final String METRIC_NAME = "quizup.websocket.messages";
    private static final String UNKNOWN_COMMAND = "UNKNOWN";

    private final MeterRegistry registry;
    private final String direction;

    public WebSocketMetricsChannelInterceptor(MeterRegistry registry, String direction) {
        this.registry = registry;
        this.direction = direction;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception exception) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        String command = accessor != null && accessor.getCommand() != null
                ? accessor.getCommand().name()
                : UNKNOWN_COMMAND;
        String result = exception == null && sent ? "sent" : "failed";

        Counter.builder(METRIC_NAME)
                .description("Trames STOMP traitées par le broker")
                .tag("direction", direction)
                .tag("command", command)
                .tag("result", result)
                .register(registry)
                .increment();
    }
}
