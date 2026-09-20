package io.github.quizup.axon.autoconfigure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;

/**
 * {@link MessageHandlerInterceptor} qui mesure le traitement d'un message Axon (commande, événement) :
 * compteur {@code result=success|failure} + timer de durée (histogramme).
 *
 * @param <M> type de message (CommandMessage / EventMessage)
 */
public class MessageMetricsHandlerInterceptor<M extends Message<?>> implements MessageHandlerInterceptor<M> {

    private final MeterRegistry registry;
    private final String counterName;
    private final String timerName;
    private final String processorName;

    public MessageMetricsHandlerInterceptor(MeterRegistry registry,
                                            String counterName,
                                            String timerName,
                                            String processorName) {
        this.registry = registry;
        this.counterName = counterName;
        this.timerName = timerName;
        this.processorName = processorName;
    }

    @Override
    public Object handle(UnitOfWork<? extends M> unitOfWork, InterceptorChain chain) throws Exception {
        String messageName = payloadName(unitOfWork.getMessage());
        Timer.Sample sample = Timer.start(registry);
        String result = "success";
        try {
            return chain.proceed();
        } catch (Exception exception) {
            result = "failure";
            throw exception;
        } finally {
            Tags tags = Tags.of("name", messageName, "result", result);
            if (processorName != null && !processorName.isBlank()) {
                tags = tags.and("processor", processorName);
            }
            Counter.builder(counterName).tags(tags).register(registry).increment();
            sample.stop(Timer.builder(timerName).tags(tags).publishPercentileHistogram().register(registry));
        }
    }

    private static String payloadName(Message<?> message) {
        if (message == null || message.getPayloadType() == null) {
            return "unknown";
        }
        return message.getPayloadType().getSimpleName();
    }
}
