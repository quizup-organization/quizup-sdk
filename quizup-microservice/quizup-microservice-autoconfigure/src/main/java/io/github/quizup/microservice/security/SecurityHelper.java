package io.github.quizup.microservice.security;

import io.github.quizup.microservice.core.domain.exception.AuthentificationProblems;
import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;
import java.util.Optional;

/**
 * Helper class for extracting information from JWT tokens
 */
public final class SecurityHelper {

    private SecurityHelper() {
        // Utility class
    }

    public static QuizUpPrincipal getPrincipal() {
        return findPrincipal()
                .orElseThrow(AuthentificationProblems.UnauthenticatedException::new);
    }

    public static Optional<QuizUpPrincipal> findPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return Optional.empty();
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String userId = jwt.getClaimAsString("user_id");
            String email = jwt.getClaimAsString("email");

            if (userId == null || userId.isBlank()) {
                return Optional.empty();
            }

            QuizUpPrincipal principal = new QuizUpPrincipal() {
                @Override
                public String getUserId() {
                    return userId;
                }

                @Override
                public String getEmail() {
                    return email;
                }
            };

            return Optional.of(principal);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof QuizUpPrincipal quizUpPrincipal) {
            return Optional.of(quizUpPrincipal);
        }

        return Optional.empty();
    }

    /**
     * Get the current JWT from the security context
     */
    public static Optional<Jwt> findJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getToken());
        }

        return Optional.empty();
    }

    public static Optional<String> findUserId() {
        return findPrincipal().map(QuizUpPrincipal::getUserId);
    }

    public static Optional<String> findUserEmail() {
        return findPrincipal().map(QuizUpPrincipal::getEmail);
    }

    /**
     * Get a specific claim from the JWT
     */
    public static Optional<String> findClaim(String claimName) {
        return findJwt()
                .map(jwt -> jwt.getClaimAsString(claimName));
    }

    /**
     * Get all claims from the JWT
     */
    public static Map<String, Object> getClaims() {
        return findJwt()
                .map(Jwt::getClaims)
                .orElseThrow(AuthentificationProblems.InvalidTokenException::new);
    }

    /**
     * Get the user ID or throw an exception if not available
     */
    public static String getUserId() {
        return findUserId()
                .orElseThrow(AuthentificationProblems.MissingUserIdException::new);
    }

    public static String getUserEmail() {
        return findUserEmail()
                .orElseThrow(AuthentificationProblems.MissingEmailException::new);
    }

    /**
     * Check if the current user is authenticated with a valid JWT
     */
    public static boolean isAuthenticated() {
        return findPrincipal().isPresent();
    }
}
