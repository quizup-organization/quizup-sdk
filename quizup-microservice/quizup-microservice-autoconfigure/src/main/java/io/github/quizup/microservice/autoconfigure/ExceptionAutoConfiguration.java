package io.github.quizup.microservice.autoconfigure;

import io.github.quizup.microservice.config.MessageHandlerConfiguration;
import io.github.quizup.microservice.exception.GlobalExceptionHandler;
import io.github.quizup.microservice.exception.ProblemCommandHandlerInterceptor;
import io.github.quizup.microservice.exception.ProblemQueryHandlerInterceptor;
import io.github.quizup.microservice.MicroserviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Configuration automatique pour la gestion globale des exceptions.
 * <p>
 * Configure automatiquement:
 * - GlobalExceptionHandler: Gestionnaire global d'exceptions REST (RFC 7807 Problem Details)
 * - ProblemCommandHandlerInterceptor: Intercepteur Axon pour transformer les exceptions de commandes
 * <p>
 * Activé par défaut, peut être désactivé avec: microservice.exception-handler.enabled=false
 * <p>
 * Exemple de configuration:
 * <pre>
 * microservice:
 *   exception-handler:
 *     enabled: true
 *     log-stack-trace: true
 *     include-binding-errors: true
 * </pre>
 * <p>
 * Le gestionnaire d'exceptions fournit des réponses uniformes selon RFC 7807 pour:
 * - CommandExecutionException (Axon)
 * - MethodArgumentNotValidException (Validation)
 * - IllegalArgumentException
 * - Exception (catch-all)
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "microservice.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({GlobalExceptionHandler.class, ProblemCommandHandlerInterceptor.class, ProblemQueryHandlerInterceptor.class, MessageHandlerConfiguration.class})
public class ExceptionAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAutoConfiguration.class);

    public ExceptionAutoConfiguration(MicroserviceProperties properties) {
        MicroserviceProperties.ExceptionHandler exceptionProps = properties.exceptionHandler();
        logger.info("ExceptionAutoConfiguration enabled - logStackTrace: {}, includeBindingErrors: {}", exceptionProps.logStackTrace(), exceptionProps.includeBindingErrors());
    }
}
