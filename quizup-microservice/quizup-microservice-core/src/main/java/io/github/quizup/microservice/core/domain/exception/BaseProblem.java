package io.github.quizup.microservice.core.domain.exception;

import java.util.Map;

/**
 * Exception de base pour les problèmes d'exécution de commande / query
 * Cette exception sera interceptée par ProblemCommandHandlerInterceptor
 * et transformée en CommandExecutionException avec les détails appropriés
 */
public abstract class BaseProblem extends RuntimeException {

    private final String type;
    private final ProblemCategory category;
    private final String title;
    private final String detail;
    private final Map<String, Object> context;

    /**
     * Constructeur complet
     */
    protected BaseProblem(
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(detail);
        this.type = type;
        this.category = category;
        this.title = title;
        this.detail = detail;
        this.context = context;
    }

    public String getType() {
        return type;
    }

    public ProblemCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public Map<String, Object> getContext() {
        return context;
    }
}
