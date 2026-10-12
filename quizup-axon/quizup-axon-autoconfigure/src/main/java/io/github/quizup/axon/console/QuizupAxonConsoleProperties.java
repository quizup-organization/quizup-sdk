package io.github.quizup.axon.console;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Console d'exploitation Axon (`quizup.axon.console`) — équivalent fonctionnel open source de la
 * Axoniq Platform (voir {@code product/axon-platform-parity.md}).
 *
 * <ul>
 *     <li>{@code enabled} — active l'<b>agent</b> de push vers {@code quizup-console}
 *     (défaut {@code false} tant que le service console n'est pas déployé) ;</li>
 *     <li>{@code url} — base URL du service console (M2M) ;</li>
 *     <li>{@code interval} — période d'envoi du snapshot (défaut 15 s) ;</li>
 *     <li>{@code include-error-details} — inclure type/message d'erreur des segments (défaut
 *     {@code true} ; endpoint interne M2M uniquement) ;</li>
 *     <li>{@code internal-api-enabled} — expose l'API interne d'exploitation
 *     {@code /internal/console/**} consommée par le service console (défaut {@code true} ;
 *     jamais ingressed, JWT M2M requis) ;</li>
 *     <li>{@code instance-id} — identifiant d'instance (défaut : variable d'environnement
 *     {@code HOSTNAME}, soit le nom du pod).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "quizup.axon.console")
public record QuizupAxonConsoleProperties(
        Boolean enabled,
        String url,
        Duration interval,
        Boolean includeErrorDetails,
        Boolean internalApiEnabled,
        String instanceId
) {

    public static final String DEFAULT_URL = "http://quizup-console.quizup-prod.svc.cluster.local";

    public QuizupAxonConsoleProperties {
        if (enabled == null) {
            enabled = Boolean.FALSE;
        }
        if (url == null || url.isBlank()) {
            url = DEFAULT_URL;
        }
        if (interval == null) {
            interval = Duration.ofSeconds(15);
        }
        if (includeErrorDetails == null) {
            includeErrorDetails = Boolean.TRUE;
        }
        if (internalApiEnabled == null) {
            internalApiEnabled = Boolean.TRUE;
        }
    }
}
