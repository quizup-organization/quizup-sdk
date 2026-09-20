package io.github.quizup.axon.autoconfigure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Comptabilise les événements publiés sur le bus ({@code quizup.axon.events.published}),
 * par type de payload.
 */
public class EventPublishMetricsDispatchInterceptor implements MessageDispatchInterceptor<EventMessage<?>> {

    private final MeterRegistry registry;

    public EventPublishMetricsDispatchInterceptor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public BiFunction<Integer, EventMessage<?>, EventMessage<?>> handle(List<? extends EventMessage<?>> messages) {
        return (index, message) -> {
            String name = message.getPayloadType() == null ? "unknown" : message.getPayloadType().getSimpleName();
            Counter.builder("quizup.axon.events.published")
                    .description("Événements publiés sur le bus")
                    .tag("name", name)
                    .register(registry)
                    .increment();
            return message;
        };
    }
}
