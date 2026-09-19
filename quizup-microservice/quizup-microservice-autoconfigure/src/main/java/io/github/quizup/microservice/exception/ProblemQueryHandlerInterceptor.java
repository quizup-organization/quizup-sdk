package io.github.quizup.microservice.exception;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.queryhandling.QueryExecutionException;
import org.axonframework.queryhandling.QueryMessage;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepteur qui transforme les CommandExecutionProblem en CommandExecutionException
 */
@Component
public class ProblemQueryHandlerInterceptor implements MessageHandlerInterceptor<QueryMessage<?, ?>> {

    @Override
    public Object handle(@NonNull UnitOfWork<? extends QueryMessage<?, ?>> unitOfWork, InterceptorChain interceptorChain) throws Exception {
        try {
            return interceptorChain.proceed();
        } catch (BaseProblem problem) {
            throw new QueryExecutionException(problem.getMessage(), problem, Map.of(
                    "type", problem.getType(),
                    "title", problem.getTitle(),
                    "detail", problem.getDetail(),
                    "category", problem.getCategory().name(),
                    "context", problem.getContext() != null ? problem.getContext() : new HashMap<>()
            ));
        }
    }
}
