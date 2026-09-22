package io.github.quizup.axon.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@AutoConfiguration
@ConditionalOnClass(Registration.class)
@AutoConfigureAfter(SimpleDiscoveryClientAutoConfiguration.class)
@EnableScheduling
public class AxonDistributedFallbackRegistrationAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(AxonDistributedFallbackRegistrationAutoConfiguration.class);

    /**
     * Fallback Registration : ne s'active que si aucun Registration n'existe déjà.
     *
     * <p>Spring Cloud Kubernetes ne fournit <b>pas</b> de {@code Registration} (en Kubernetes,
     * l'enregistrement est géré par la plateforme, cf. sa documentation « Service Registry
     * Implementation »). Ce fallback est donc la source du {@code Registration} pour le
     * {@code SpringCloudCommandRouter} d'Axon — y compris en production.</p>
     *
     * <p>L'adresse du {@code Registration} doit correspondre à celle annoncée par le
     * {@code DiscoveryClient}, sinon le routeur traite l'instance locale comme distante :
     * il récupère ses capacités par HTTP ({@code /command-capabilities}) au lieu de la mémoire,
     * et si ce handshake n'est pas prêt (démarrage), le nœud local se déclare incapable
     * ({@code No node known to accept command}). En Kubernetes on utilise donc l'IP locale du
     * pod ({@code spring.cloud.client.ip-address}) ; en local (SimpleDiscoveryClient) on garde
     * {@code localhost} pour matcher {@code spring.cloud.discovery.client.simple.instances}.</p>
     */
    @Bean
    @ConditionalOnMissingBean(Registration.class)
    public Registration axonDistributedFallbackRegistration(
            ObjectProvider<DiscoveryClient> discoveryClientProvider,
            Environment environment,
            @Value("${spring.application.name:axon-distributed-application}") String serviceId,
            @Value("${server.address:}") String configuredHost,
            @Value("${server.port:8080}") int port) {
        return new SimpleRegistration(serviceId, resolveLocalHost(discoveryClientProvider, environment, configuredHost), port);
    }

    private String resolveLocalHost(ObjectProvider<DiscoveryClient> discoveryClientProvider,
                                    Environment environment,
                                    String configuredHost) {
        if (StringUtils.hasText(configuredHost)) {
            return configuredHost;
        }
        boolean usesSimpleDiscovery = discoveryClientProvider.stream()
                .anyMatch(this::containsSimpleDiscoveryClient);
        if (usesSimpleDiscovery) {
            return "localhost";
        }
        String localIp = environment.getProperty("spring.cloud.client.ip-address");
        if (StringUtils.hasText(localIp)) {
            return localIp;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            logger.warn("Could not resolve local host address, falling back to localhost: {}",
                    exception.getMessage());
            return "localhost";
        }
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