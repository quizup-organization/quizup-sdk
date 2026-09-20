package io.github.quizup.axon.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@AutoConfiguration
@ConditionalOnClass(Registration.class)
@AutoConfigureAfter(SimpleDiscoveryClientAutoConfiguration.class)
@EnableScheduling
public class AxonDistributedFallbackRegistrationAutoConfiguration {

    /**
     * Fallback Registration : ne s'active que si aucun Registration n'existe déjà.
     *
     * <p>Spring Cloud Kubernetes ne fournit <b>pas</b> de {@code Registration} (en Kubernetes,
     * l'enregistrement est géré par la plateforme, cf. sa documentation « Service Registry
     * Implementation »). Ce fallback est donc la source du {@code Registration} pour le
     * {@code SpringCloudCommandRouter} d'Axon. Conséquence connue : l'instance locale peut
     * ne pas être reconnue comme telle par le routeur (host/port différents de l'instance
     * découverte) — sans impact ici, les queries étant résolues en local d'abord et
     * {@code scatterGather} n'étant pas utilisé.</p>
     */
    @Bean
    @ConditionalOnMissingBean(Registration.class)
    public Registration axonDistributedFallbackRegistration(
            @Value("${spring.application.name:axon-distributed-application}") String serviceId,
            @Value("${server.address:localhost}") String host,
            @Value("${server.port:8080}") int port) {
        return new SimpleRegistration(serviceId, host, port);
    }

    /**
     * SimpleDiscoveryClient (utilisé en dev/local) est passif : il ne publie jamais
     * de HeartbeatEvent tout seul, contrairement à Eureka, Consul ou Kubernetes
     * Discovery en prod (qui ont leurs propres mécanismes natifs). On émet donc
     * manuellement un HeartbeatEvent uniquement quand SimpleDiscoveryClient est
     * détecté - y compris lorsqu'il est encapsulé dans un CompositeDiscoveryClient.
     *
     * En prod avec KubernetesDiscoveryClient, ce bean ne se créera jamais, car ce
     * n'est pas une instance de SimpleDiscoveryClient - Kubernetes gère déjà ses
     * propres événements de heartbeat.
     */
    @Bean
    public SimpleDiscoveryHeartbeatEmitter simpleDiscoveryHeartbeatEmitter(
            ObjectProvider<DiscoveryClient> discoveryClientProvider,
            ApplicationEventPublisher publisher) {

        boolean usesSimpleDiscovery = discoveryClientProvider.stream()
                .anyMatch(this::containsSimpleDiscoveryClient);

        if (usesSimpleDiscovery) {
            SimpleDiscoveryHeartbeatEmitter emitter = new SimpleDiscoveryHeartbeatEmitter(publisher);
            emitter.emitHeartbeat();
            return emitter;
        } else {
            return null;
        }
    }

    private boolean containsSimpleDiscoveryClient(DiscoveryClient client) {
        if (client instanceof SimpleDiscoveryClient) {
            return true;
        }
        if (client instanceof CompositeDiscoveryClient composite) {
            List<DiscoveryClient> delegates = composite.getDiscoveryClients();
            return delegates.stream().anyMatch(SimpleDiscoveryClient.class::isInstance);
        }
        return false;
    }

    public record SimpleRegistration(String serviceId, String host, int port) implements Registration {

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public URI getUri() {
            return URI.create("http://" + host + ":" + port);
        }

        @Override
        public Map<String, String> getMetadata() {
            return Collections.emptyMap();
        }
    }

    public static class SimpleDiscoveryHeartbeatEmitter {

        private final ApplicationEventPublisher publisher;
        private final AtomicInteger heartbeatState = new AtomicInteger(0);

        SimpleDiscoveryHeartbeatEmitter(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        /**
         * SimpleDiscoveryClient ne publie pas de HeartbeatEvent nativement (contrairement
         * à Eureka/Consul/Kubernetes). On ré-émet donc périodiquement un heartbeat pour que
         * le {@code SpringCloudCommandRouter} rafraîchisse ses capacités de routage dès que
         * les autres noeuds sont disponibles (sans cela, un service démarré avant ses pairs
         * reste bloqué sur la liste découverte au boot → {@code No node known to accept command}).
         */
        @Scheduled(
                fixedDelayString = "${axon.distributed.spring-cloud.heartbeat-interval:10000}",
                initialDelayString = "${axon.distributed.spring-cloud.heartbeat-interval:10000}")
        public void emitHeartbeat() {
            publisher.publishEvent(new HeartbeatEvent(this, heartbeatState.incrementAndGet()));
        }
    }
}