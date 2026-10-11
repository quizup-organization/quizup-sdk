package io.github.quizup.axon.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Groupes de traitement **propres à chaque instance** (`quizup.axon.instance-scoped-groups`).
 *
 * <p>Pour un service à fan-out temps réel (ex. le BFF : chaque réplique ne connaît que les clients
 * STOMP qui lui sont connectés), un processor partagé par segments ne suffit pas : il faut que
 * <b>chaque instance</b> consomme <b>tous</b> les événements. Chaque groupe listé ici reçoit un
 * processor suffixé par l'identifiant d'instance (`<groupe>-<instanceId>`) — donc son propre token
 * dans le token store — et démarre à la <b>queue</b> du flux pour un nouveau pod (les clients
 * complètent l'historique via les folds REST, aucun replay Kafka massif).</p>
 *
 * <p>{@code instanceId} : identifiant stable de l'instance (défaut : variable d'environnement
 * {@code HOSTNAME}, soit le nom du pod Kubernetes).</p>
 */
@ConfigurationProperties(prefix = "quizup.axon")
public record InstanceScopedProcessingGroupsProperties(
        List<String> instanceScopedGroups,
        String instanceId
) {

    public InstanceScopedProcessingGroupsProperties {
        if (instanceScopedGroups == null) {
            instanceScopedGroups = List.of();
        }
    }
}
