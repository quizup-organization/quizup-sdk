package io.github.quizup.axon.console;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Suivi des <b>commandes en vol</b> (contenu natif Axon Server : « long-running commands »).
 * Intercepteur de handler de commande enregistré sur le command bus local ; expose, par type de
 * commande, le nombre en cours et l'âge de la plus ancienne.
 *
 * <p>Axon Framework ne permet pas d'annuler une commande en cours — la console les affiche
 * uniquement (diagnostic des blocages).</p>
 */
public class CommandActivityTracker implements MessageHandlerInterceptor<CommandMessage<?>> {

    private final Map<String, InFlight> inFlight = new ConcurrentHashMap<>();

    @Override
    public Object handle(UnitOfWork<? extends CommandMessage<?>> unitOfWork, InterceptorChain chain) throws Exception {
        CommandMessage<?> message = unitOfWork.getMessage();
        String name = message.getPayloadType() != null ? message.getPayloadType().getName() : "unknown";
        InFlight entry = inFlight.computeIfAbsent(name, key -> new InFlight());
        entry.start();
        try {
            return chain.proceed();
        } finally {
            entry.finish();
        }
    }

    public List<InFlightCommand> snapshot() {
        return inFlight.entrySet()
                .stream()
                .filter(entry -> entry.getValue().count() > 0)
                .map(entry -> new InFlightCommand(
                        entry.getKey(), entry.getValue().count(),
                        entry.getValue().oldestStart(), entry.getValue().lastStart()))
                .sorted((left, right) -> {
                    Long leftOldest = left.oldestStart() == null ? Long.MAX_VALUE : left.oldestStart().toEpochMilli();
                    Long rightOldest = right.oldestStart() == null ? Long.MAX_VALUE : right.oldestStart().toEpochMilli();
                    return Long.compare(leftOldest, rightOldest);
                })
                .toList();
    }

    private static final class InFlight {

        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant oldestStart;
        private final AtomicReference<Instant> lastStart = new AtomicReference<>();

        synchronized void start() {
            if (count.getAndIncrement() == 0) {
                oldestStart = Instant.now();
            }
            lastStart.set(Instant.now());
        }

        synchronized void finish() {
            if (count.decrementAndGet() <= 0) {
                count.set(0);
                oldestStart = null;
            }
        }

        int count() {
            return count.get();
        }

        Instant oldestStart() {
            return oldestStart;
        }

        Instant lastStart() {
            return lastStart.get();
        }
    }

    public record InFlightCommand(String name, int count, Instant oldestStart, Instant lastStart) {
    }
}
