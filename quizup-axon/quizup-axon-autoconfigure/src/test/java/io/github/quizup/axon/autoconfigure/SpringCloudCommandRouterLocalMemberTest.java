package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.autoconfigure.AxonDistributedFallbackRegistrationAutoConfiguration.SimpleRegistration;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.distributed.CommandMessageFilter;
import org.axonframework.commandhandling.distributed.Member;
import org.axonframework.extensions.springcloud.commandhandling.SpringCloudCommandRouter;
import org.axonframework.extensions.springcloud.commandhandling.mode.CapabilityDiscoveryMode;
import org.axonframework.extensions.springcloud.commandhandling.mode.DefaultMemberCapabilities;
import org.axonframework.extensions.springcloud.commandhandling.mode.MemberCapabilities;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduit localement la race de démarrage Kubernetes :
 *
 * <p>{@code SpringCloudCommandRouter.updateMemberships()} (déclenché par un {@code HeartbeatEvent})
 * reconstruit le {@code ConsistentHash} à partir du {@code DiscoveryClient}. Tant que le pod local
 * n'est pas {@code Ready}, il est absent des Endpoints : le membre local est alors <b>retiré</b> et
 * une commande pourtant locale n'a plus de destination → {@code No node known to accept command}
 * (ce qui faisait échouer les seeders en prod, alors qu'en local SimpleDiscovery expose toujours
 * l'instance).</p>
 *
 * <p>Le correctif SDK active {@code spring.cloud.kubernetes.discovery.include-not-ready-addresses}
 * pour que le pod local soit découvert dès sa création (testé dans
 * {@link AxonDistributedEnvironmentPostProcessorTest}). Le présent test verrouille l'invariant de
 * routage : « si l'instance locale est découverte, la commande locale est routée localement ; sinon
 * elle est perdue ».</p>
 */
class SpringCloudCommandRouterLocalMemberTest {

    private static final String LOCAL_SERVICE = "quizup-theme";
    private static final String LOCAL_HOST = "10.42.0.171";
    private static final int LOCAL_PORT = 8080;

    @Test
    void localCommandLosesItsDestinationWhenTheLocalPodIsNotDiscovered() {
        DiscoveryClient discovery = discovery(
                Set.of(LOCAL_SERVICE, "quizup-game"),
                Map.of("quizup-game", List.of(instance("quizup-game", "10.42.1.198", 8080))));

        SpringCloudCommandRouter router = router(discovery);
        router.updateMembership(100, acceptAll());
        router.updateMemberships(new HeartbeatEvent(this, 1));

        assertThat(router.findDestination(command()))
                .as("membre local absent de la découverte → aucune destination")
                .isEmpty();
    }

    @Test
    void localCommandIsRoutedLocallyWhenTheLocalPodIsDiscovered() {
        DiscoveryClient discovery = discovery(
                Set.of(LOCAL_SERVICE),
                Map.of(LOCAL_SERVICE, List.of(instance(LOCAL_SERVICE, LOCAL_HOST, LOCAL_PORT))));

        SpringCloudCommandRouter router = router(discovery);
        router.updateMembership(100, acceptAll());
        router.updateMemberships(new HeartbeatEvent(this, 1));

        Optional<Member> destination = router.findDestination(command());
        assertThat(destination).as("l'instance locale découverte restore la destination").isPresent();
        assertThat(destination.get().local()).isTrue();
    }

    private SpringCloudCommandRouter router(DiscoveryClient discovery) {
        Registration registration = new SimpleRegistration(LOCAL_SERVICE, LOCAL_HOST, LOCAL_PORT);
        return SpringCloudCommandRouter.builder()
                .discoveryClient(discovery)
                .localServiceInstance(registration)
                .routingStrategy(commandMessage -> "theme-1")
                .capabilityDiscoveryMode(new FakeCapabilityDiscoveryMode())
                .serializer(XStreamSerializer.defaultSerializer())
                .build();
    }

    private static GenericCommandMessage<ThemeCommand> command() {
        return new GenericCommandMessage<>(new ThemeCommand());
    }

    private static CommandMessageFilter acceptAll() {
        return commandMessage -> true;
    }

    private static ServiceInstance instance(String serviceId, String host, int port) {
        return new DefaultServiceInstance(serviceId + "-" + host, serviceId, host, port, false);
    }

    private static DiscoveryClient discovery(Set<String> services, Map<String, List<ServiceInstance>> instances) {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "fake discovery";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                return instances.getOrDefault(serviceId, List.of());
            }

            @Override
            public List<String> getServices() {
                return List.copyOf(services);
            }
        };
    }

    /** Le nœud local (theme) accepte les commandes "Theme" ; les pairs (game) acceptent "Game". */
    private static final class FakeCapabilityDiscoveryMode implements CapabilityDiscoveryMode {

        @Override
        public void updateLocalCapabilities(ServiceInstance localInstance, int loadFactor,
                                            CommandMessageFilter commandFilter) {
            // Les capacités locales sont fournies par capabilities(...) ci-dessous.
        }

        @Override
        public Optional<MemberCapabilities> capabilities(ServiceInstance serviceInstance) {
            boolean local = LOCAL_SERVICE.equals(serviceInstance.getServiceId());
            CommandMessageFilter filter = commandMessage ->
                    commandMessage.getCommandName().contains(local ? "Theme" : "Game");
            return Optional.of(new DefaultMemberCapabilities(100, filter));
        }
    }

    static final class ThemeCommand {
        @Override
        public String toString() {
            return "ThemeCommand";
        }
    }
}
