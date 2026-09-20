package io.github.quizup.microservice.core.domain.constant;

/**
 * Constantes globales partagées entre tous les modules QuizUp.
 * Fournit les identifiants système utilisés pour le seeding et les opérations internes.
 *
 * <p>Le compte système est <b>unique</b> : il sert à la fois de créateur/administrateur
 * (topics, questions) et d'adversaire bot (parties, matchmaking). Il ne possède aucun
 * credential et ne peut pas se connecter.</p>
 */
public interface QuizUpConstants {

    /** Email de contact public de la plateforme (aussi email du compte système). */
    String CONTACT_EMAIL = "quizup.contacts@gmail.com";

    /** Identifiant du compte système unique (admin + bot). */
    String SYSTEM_USER_ID = "0ada9a20-2198-4014-9ed0-57d0cc82fb42";

    /** Nom d'affichage du compte système (visible comme adversaire bot). */
    String SYSTEM_USER_NAME = "QuizMeUp Bot";

    /** Email du compte système (non connectable : exclu du flux passwordless). */
    String SYSTEM_USER_EMAIL = CONTACT_EMAIL;
}
