package io.github.quizup.axon.console;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Activation et cadrage de la <b>Dead-Letter Queue</b> Axon (`quizup.axon.dlq`).
 *
 * <p>La DLQ est <b>activée par défaut</b> pour tous les processing groups kebab-case : un événement
 * dont le handler échoue est alors <b>enqueued</b> (politique par défaut d'Axon) au lieu de bloquer
 * indéfiniment le segment du processor — c'est la correction du « poison event ».</p>
 *
 * <p>Sont automatiquement exclus : les groupes <b>instance-scopés</b>
 * ({@code quizup.axon.instance-scoped-groups}, suffixés par pod — une DLQ par pod orphelinerait les
 * lignes), les groupes dont le nom contient un point (FQCN de sagas — Axon ne wrappe pas les sagas
 * avec la DLQ), et la liste {@link #excludedGroups()}.</p>
 *
 * <ul>
 *     <li>{@code enabled} — défaut {@code true} ;</li>
 *     <li>{@code excluded-groups} — exclusions explicites supplémentaires ;</li>
 *     <li>{@code max-sequences} / {@code max-sequence-size} — bornes de la queue
 *     (défauts Axon si absents) ;</li>
 *     <li>{@code claim-duration} — durée de claim d'une séquence (défaut Axon si absent).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "quizup.axon.dlq")
public record QuizupAxonDlqProperties(
        Boolean enabled,
        List<String> excludedGroups,
        Integer maxSequences,
        Integer maxSequenceSize,
        Duration claimDuration
) {

    public QuizupAxonDlqProperties {
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
        if (excludedGroups == null) {
            excludedGroups = List.of();
        }
    }
}
