package io.github.quizup.microservice.core.domain.exception;

public interface AuthentificationProblems {

    /**
     * L'utilisateur n'est pas authentifié (pas de contexte de sécurité ou authentification absente)
     */
    class UnauthenticatedException extends AuthentificationProblem {
        public UnauthenticatedException() {
            super("urn:quizup:authentification:unauthenticated", "Not authenticated", "No authentication context found. Please log in.");
        }
    }

    /**
     * Le JWT est présent, mais invalide (mauvais format, signature invalide, expiré, etc.)
     */
    class InvalidTokenException extends AuthentificationProblem {
        public InvalidTokenException() {
            super("urn:quizup:authentification:invalidToken", "Invalid token", "The provided JWT token is invalid");
        }
    }

    /**
     * Le claim user_id est absent ou vide dans le JWT
     */
    class MissingUserIdException extends AuthentificationProblem {
        public MissingUserIdException() {
            super("urn:quizup:authentification:missingUserId", "Missing user identifier", "The JWT token does not contain a valid user_id claim");
        }
    }

    /**
     * Le claim email est absent ou vide dans le JWT
     */
    class MissingEmailException extends AuthentificationProblem {
        public MissingEmailException() {
            super("urn:quizup:authentification:missingEmail", "Missing user email", "The JWT token does not contain a valid email claim");
        }
    }
}
