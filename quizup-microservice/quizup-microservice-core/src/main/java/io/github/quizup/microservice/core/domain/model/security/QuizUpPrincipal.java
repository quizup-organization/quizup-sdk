package io.github.quizup.microservice.core.domain.model.security;

import java.security.Principal;

public interface QuizUpPrincipal extends Principal {

    /**
     * @return L'identifiant unique de l'utilisateur dans QuizUp
     */
    String getUserId();

    /**
     * @return L'email de l'utilisateur
     */
    String getEmail();

    @Override
    default String getName() {
        return getUserId();
    }
}
