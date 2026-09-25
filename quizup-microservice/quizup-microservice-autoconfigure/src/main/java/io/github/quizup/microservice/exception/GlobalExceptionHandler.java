package io.github.quizup.microservice.exception;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;
import io.github.quizup.microservice.core.infrastructure.in.api.response.ExceptionResponse;
import io.github.quizup.microservice.MicroserviceProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.messaging.HandlerExecutionException;
import org.axonframework.queryhandling.QueryExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Gestionnaire global d'exceptions pour tous les controllers REST.
 * <p>
 * Fournit des réponses d'erreur uniformes selon RFC 7807 (Problem Details for HTTP APIs).
 * <p>
 * Gère les types d'exceptions suivants:
 * - CommandExecutionException: Exceptions des commandes Axon (transformées depuis CommandExecutionProblem)
 * - MethodArgumentNotValidException: Erreurs de validation Bean Validation
 * - IllegalArgumentException: Arguments invalides
 * - NoResourceFoundException: Ressources statiques inexistantes (404, sans log d'erreur)
 * - Exception: Catch-all pour toutes les exceptions non gérées
 * <p>
 * Configuration disponible via microservice.exception-handler:
 * - enabled: Activer/désactiver le gestionnaire (défaut: true)
 * - log-stack-trace: Logger la stack trace complète (défaut: true)
 * - include-binding-errors: Inclure les erreurs de binding dans les réponses (défaut: true)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MicroserviceProperties.ExceptionHandler properties;

    public GlobalExceptionHandler(MicroserviceProperties microserviceProperties) {
        this.properties = microserviceProperties.exceptionHandler();
    }

    /**
     * Gère les CommandExecutionException d'Axon (transformées depuis CommandExecutionProblem)
     */
    @ExceptionHandler(CommandExecutionException.class)
    public ResponseEntity<ExceptionResponse> handleCommandExecutionException(CommandExecutionException ex, HttpServletRequest request) {
        return handleHandlerExecutionException(ex, request);
    }

    @ExceptionHandler(QueryExecutionException.class)
    public ResponseEntity<ExceptionResponse> handleQueryExecutionException(QueryExecutionException ex, HttpServletRequest request) {
        return handleHandlerExecutionException(ex, request);
    }

    /**
     * Gère les erreurs de validation des arguments de méthode
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, Object> context = new HashMap<>();

        if (properties.includeBindingErrors()) {
            Map<String, Object> validationErrors = new HashMap<>();
            ex.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                validationErrors.put(fieldName, errorMessage);
            });
            context.put("errors", validationErrors);

            logger.warn("Validation error: {}", validationErrors);
        } else {
            logger.warn("Validation error occurred - {} field(s) failed validation",
                    ex.getBindingResult().getErrorCount());
        }

        ExceptionResponse response = new ExceptionResponse(
                "validation-error",
                ProblemCategory.VALIDATION,
                "Validation Failed",
                "Les données fournies ne sont pas valides",
                context.isEmpty() ? null : context,
                HttpStatus.BAD_REQUEST.value(),
                request.getDescription(false).replace("uri=", "")
        );

        return buildResponse(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Gère IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        ExceptionResponse response = new ExceptionResponse(
                "illegal-argument",
                ProblemCategory.BUSINESS_INVALID_COMMAND,
                "Argument Invalide",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getDescription(false).replace("uri=", "")
        );

        if (properties.logStackTrace()) {
            logger.warn("Illegal argument", ex);
        } else {
            logger.warn("Illegal argument: {}", ex.getMessage());
        }

        return buildResponse(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Gère les requêtes vers des ressources statiques inexistantes (favicon.ico, etc.)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNoResourceFoundException(
            NoResourceFoundException ex,
            WebRequest request) {

        ExceptionResponse response = new ExceptionResponse(
                "not-found",
                ProblemCategory.BUSINESS_RESOURCE_MISSING,
                "Ressource Introuvable",
                "La ressource demandée n'existe pas: " + ex.getResourcePath(),
                HttpStatus.NOT_FOUND.value(),
                request.getDescription(false).replace("uri=", "")
        );

        logger.debug("Resource not found: {}", ex.getResourcePath());

        return buildResponse(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Gère toutes les exceptions non gérées
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        ExceptionResponse response = new ExceptionResponse(
                "internal-server-error",
                ProblemCategory.TECHNICAL,
                "Erreur Interne du Serveur",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getDescription(false).replace("uri=", "")
        );

        if (properties.logStackTrace()) {
            logger.error("Unexpected error occurred", ex);
        } else {
            logger.error("Unexpected error occurred: {}", ex.getMessage());
        }

        return buildResponse(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Construit une ResponseEntity avec les headers appropriés pour RFC 7807
     */
    private ResponseEntity<ExceptionResponse> buildResponse(ExceptionResponse response, HttpStatus httpStatus) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(response, headers, httpStatus);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<ExceptionResponse> handleHandlerExecutionException(HandlerExecutionException ex, HttpServletRequest request) {
        // Over the distributed bus, the structured Problem details are lost: Axon only transports the
        // exception type + message + cause chain. Unwrap the cause chain to recover the original
        // BaseProblem and map its category to the right HTTP status (e.g. NOT_FOUND) instead of a 500.
        Optional<BaseProblem> problemOpt = findBaseProblem(ex);
        if (problemOpt.isPresent()) {
            BaseProblem problem = problemOpt.get();
            HttpStatus httpStatus = statusForCategory(problem.getCategory());

            ExceptionResponse response = new ExceptionResponse(
                    problem.getType(),
                    problem.getCategory(),
                    problem.getTitle(),
                    problem.getDetail(),
                    problem.getContext(),
                    httpStatus.value(),
                    request.getRequestURI()
            );

            if (properties.logStackTrace()) {
                logger.error("handler execution problem: type={}, category={}", problem.getType(),
                        problem.getCategory(), ex);
            } else {
                logger.error("handler execution problem: type={}, category={}, message={}",
                        problem.getType(), problem.getCategory(), problem.getDetail());
            }

            return buildResponse(response, httpStatus);
        }

        Optional<Map<String, Object>> detailsOpt = ex.getDetails()
                .filter(details -> details instanceof Map)
                .map(details -> (Map<String, Object>) details);

        if (detailsOpt.isPresent()) {
            Map<String, Object> details = detailsOpt.get();

            // Extraire les informations du problème
            String type = (String) details.get("type");
            String title = (String) details.get("title");
            String detail = (String) details.get("detail");
            String categoryStr = (String) details.get("category");
            Map<String, Object> context = (Map<String, Object>) details.get("context");

            // Déterminer le status HTTP en fonction de la catégorie
            ProblemCategory category;
            HttpStatus httpStatus;
            try {
                category = ProblemCategory.valueOf(categoryStr);
                httpStatus = switch (category) {
                    case BUSINESS_INVALID_COMMAND, VALIDATION -> HttpStatus.BAD_REQUEST;
                    case PERMISSION -> HttpStatus.FORBIDDEN;
                    case BUSINESS_AGGREGATE, TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
                    case BUSINESS_RESOURCE_MISSING -> HttpStatus.NOT_FOUND;
                };
            } catch (Exception e) {
                category = ProblemCategory.TECHNICAL;
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }

            ExceptionResponse response = new ExceptionResponse(
                    type,
                    category,
                    title,
                    detail,
                    context,
                    httpStatus.value(),
                    request.getRequestURI()
            );

            if (properties.logStackTrace()) {
                logger.error("handler execution error: type={}, category={}", type, category, ex);
            } else {
                logger.error("handler execution error: type={}, category={}, message={}", type, category, ex.getMessage());
            }

            return buildResponse(response, httpStatus);
        }

        // Si pas de détails structurés, erreur générique
        if (properties.logStackTrace()) {
            logger.error("Could not handle {}", request.getRequestURI(), ex);
        } else {
            logger.error("Could not handle {} - message: {}", request.getRequestURI(), ex.getMessage());
        }

        ExceptionResponse response = new ExceptionResponse(
                "unknown-command-error",
                ProblemCategory.TECHNICAL,
                "Erreur lors de l'exécution de la commande",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );

        return buildResponse(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Reconstruit le {@link BaseProblem} d'origine en parcourant la chaîne de causes. Indispensable
     * pour les exceptions issues du bus distribué, où seuls le type, le message et la cause sont
     * transportés (les détails structurés ne survivent pas).
     */
    private Optional<BaseProblem> findBaseProblem(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BaseProblem problem) {
                return Optional.of(problem);
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private HttpStatus statusForCategory(ProblemCategory category) {
        if (category == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (category) {
            case BUSINESS_INVALID_COMMAND, VALIDATION -> HttpStatus.BAD_REQUEST;
            case PERMISSION -> HttpStatus.FORBIDDEN;
            case BUSINESS_RESOURCE_MISSING -> HttpStatus.NOT_FOUND;
            case BUSINESS_AGGREGATE, TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

}
