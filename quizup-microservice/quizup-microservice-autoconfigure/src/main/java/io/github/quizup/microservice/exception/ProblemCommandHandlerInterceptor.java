package io.github.quizup.microservice.exception;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepteur qui transforme les CommandExecutionProblem en CommandExecutionException
 */
@Component
public class ProblemCommandHandlerInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {

    @Override
    public Object handle(@NonNull UnitOfWork<? extends CommandMessage<?>> unitOfWork, InterceptorChain interceptorChain) throws Exception {
        try {
            return interceptorChain.proceed();
        } catch (BaseProblem problem) {
            throw new CommandExecutionException(problem.getMessage(), problem, Map.of(
                "type", problem.getType(),
                "title", problem.getTitle(),
                "detail", problem.getDetail(),
                "category", problem.getCategory().name(),
                "context", problem.getContext() != null ? problem.getContext() : new HashMap<>()
            ));
        }
    }
}
