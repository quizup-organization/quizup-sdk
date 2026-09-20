package io.github.quizup.axon.autoconfigure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class AxonActivityMetricsInterceptorsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void countsSuccessfulCommandWithDuration() throws Exception {
        MessageMetricsHandlerInterceptor<CommandMessage<?>> interceptor =
                new MessageMetricsHandlerInterceptor<>(registry, "quizup.axon.commands",
                        "quizup.axon.command.duration", null);

        UnitOfWork unitOfWork = mock(UnitOfWork.class);
        CommandMessage<?> message = mock(CommandMessage.class);
        when(unitOfWork.getMessage()).thenReturn(message);
        when(message.getPayloadType()).thenReturn((Class) String.class);
        InterceptorChain chain = mock(InterceptorChain.class);
        when(chain.proceed()).thenReturn("ok");

        interceptor.handle(unitOfWork, chain);

        assertThat(registry.get("quizup.axon.commands")
                .tags("name", "String", "result", "success").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("quizup.axon.command.duration")
                .tags("name", "String", "result", "success").timer().count()).isEqualTo(1);
    }

    @Test
    void countsFailedEventWithProcessorTag() throws Exception {
        MessageMetricsHandlerInterceptor<EventMessage<?>> interceptor =
                new MessageMetricsHandlerInterceptor<>(registry, "quizup.axon.events.processed",
                        "quizup.axon.event.duration", "quizup-game-projection");

        UnitOfWork unitOfWork = mock(UnitOfWork.class);
        EventMessage<?> message = mock(EventMessage.class);
        when(unitOfWork.getMessage()).thenReturn(message);
        when(message.getPayloadType()).thenReturn((Class) Integer.class);
        InterceptorChain chain = mock(InterceptorChain.class);
        when(chain.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> interceptor.handle(unitOfWork, chain)).isInstanceOf(IllegalStateException.class);

        assertThat(registry.get("quizup.axon.events.processed")
                .tags("name", "Integer", "result", "failure", "processor", "quizup-game-projection")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void countsPublishedEvents() {
        EventPublishMetricsDispatchInterceptor interceptor = new EventPublishMetricsDispatchInterceptor(registry);
        EventMessage<?> message = mock(EventMessage.class);
        when(message.getPayloadType()).thenReturn((Class) Long.class);

        interceptor.handle(List.of(message)).apply(0, message);

        assertThat(registry.get("quizup.axon.events.published").tag("name", "Long").counter().count())
                .isEqualTo(1.0);
    }
}
