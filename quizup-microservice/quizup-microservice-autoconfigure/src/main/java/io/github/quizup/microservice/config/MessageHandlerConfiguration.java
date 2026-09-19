package io.github.quizup.microservice.config;

import io.github.quizup.microservice.exception.ProblemCommandHandlerInterceptor;
import io.github.quizup.microservice.exception.ProblemQueryHandlerInterceptor;
import jakarta.annotation.PreDestroy;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.common.Registration;
import org.axonframework.queryhandling.QueryBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import jakarta.annotation.PostConstruct;

@Configuration
public class MessageHandlerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandlerConfiguration.class);

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final ProblemCommandHandlerInterceptor commandInterceptor;
    private final ProblemQueryHandlerInterceptor queryInterceptor;
    private Registration commandInterceptorRegistration;
    private Registration queryInterceptorRegistration;

    @Autowired
    public MessageHandlerConfiguration(
            @Lazy @Qualifier("distributedCommandBus") CommandBus commandBus,
            @Lazy @Qualifier("distributedQueryBus") QueryBus queryBus,
            ProblemCommandHandlerInterceptor commandInterceptor,
            ProblemQueryHandlerInterceptor queryInterceptor) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
        this.commandInterceptor = commandInterceptor;
        this.queryInterceptor = queryInterceptor;
    }

    /**
     * Enregistre les intercepteurs après l'initialisation complète de Spring
     */
    @PostConstruct
    public void registerInterceptors() {
        commandInterceptorRegistration = commandBus.registerHandlerInterceptor(commandInterceptor);
        logger.info("ProblemCommandHandlerInterceptor registered with Axon CommandBus");

        queryInterceptorRegistration = queryBus.registerHandlerInterceptor(queryInterceptor);
        logger.info("ProblemQueryHandlerInterceptor registered with Axon QueryBus");
    }

    /**
     * Désenregistre les intercepteurs avant la destruction du bean pour éviter les fuites de mémoire
     */
    @PreDestroy
    public void unregisterInterceptors() {
        if (commandInterceptorRegistration != null) {
            commandInterceptorRegistration.close();
            logger.info("ProblemCommandHandlerInterceptor unregistered from Axon CommandBus");
        }
        if (queryInterceptorRegistration != null) {
            queryInterceptorRegistration.close();
            logger.info("ProblemQueryHandlerInterceptor unregistered from Axon QueryBus");
        }
    }
}
