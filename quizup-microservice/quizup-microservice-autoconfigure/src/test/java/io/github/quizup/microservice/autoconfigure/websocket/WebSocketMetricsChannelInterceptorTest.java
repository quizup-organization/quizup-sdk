package io.github.quizup.microservice.autoconfigure.websocket;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketMetricsChannelInterceptorTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final WebSocketMetricsChannelInterceptor interceptor =
            new WebSocketMetricsChannelInterceptor(registry, "outbound");

    @Test
    void countsSuccessfulStompMessages() {
        interceptor.afterSendCompletion(message(StompCommand.MESSAGE), null, true, null);

        assertThat(registry.get("quizup.websocket.messages")
                .tags("direction", "outbound", "command", "MESSAGE", "result", "sent")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void countsFailedStompMessages() {
        interceptor.afterSendCompletion(message(StompCommand.SEND), null, false, new IllegalStateException("boom"));

        assertThat(registry.get("quizup.websocket.messages")
                .tags("direction", "outbound", "command", "SEND", "result", "failed")
                .counter()
                .count()).isEqualTo(1.0);
    }

    private Message<byte[]> message(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
