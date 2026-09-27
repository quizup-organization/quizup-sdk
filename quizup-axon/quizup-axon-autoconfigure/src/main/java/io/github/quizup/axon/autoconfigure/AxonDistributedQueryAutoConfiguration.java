package io.github.quizup.axon.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.axon.query.HttpQueryBusConnector;
import io.github.quizup.axon.query.QueryCapabilityRegistry;
import io.github.quizup.axon.query.SpringCloudDistributedQueryBus;
import io.github.quizup.axon.query.SpringCloudQueryRouter;
import io.github.quizup.axon.query.api.QueryCapabilitiesController;
import io.github.quizup.axon.query.api.QueryTransportController;
import io.github.quizup.axon.query.message.EventEnvelopeModule;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.serialization.RevisionResolver;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.springboot.autoconfig.AxonAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@AutoConfiguration(after = AxonAutoConfiguration.class)
public class AxonDistributedQueryAutoConfiguration {

    @Bean
    public QueryCapabilityRegistry queryCapabilityRegistry() {
        return new QueryCapabilityRegistry();
    }

    @Bean("axonKafkaQueryDispatchExecutor")
    public Executor axonKafkaQueryDispatchExecutor() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("axon-kafka-query-dispatch");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Serializer dédié au query bus distribué : copie de l'{@link ObjectMapper} applicatif enrichie
     * du codec {@link EventEnvelopeModule} (payload typé via {@code eventType}). Il est détenu hors
     * du type {@link Serializer} pour ne pas perturber les conditions d'Axon sur les serializers
     * général/message/event, qui restent inchangés (format event store préservé).
     */
    @Bean
    public DistributedQuerySerializer distributedQuerySerializer(ObjectMapper objectMapper,
                                                                 RevisionResolver revisionResolver) {
        ObjectMapper queryObjectMapper = objectMapper.copy();
        queryObjectMapper.registerModule(new EventEnvelopeModule());
        return new DistributedQuerySerializer(JacksonSerializer.builder()
                .objectMapper(queryObjectMapper)
                .revisionResolver(revisionResolver)
                .build());
    }

    @Bean
    public HttpQueryBusConnector httpQueryBusConnector(RestTemplate restTemplate,
                                                       DistributedQuerySerializer querySerializer) {
        return new HttpQueryBusConnector(restTemplate, querySerializer.serializer());
    }

    @Bean
    public SpringCloudQueryRouter springCloudQueryRouter(DiscoveryClient discoveryClient,
                                                         Registration registration,
                                                         HttpQueryBusConnector connector) {
        return new SpringCloudQueryRouter(discoveryClient, registration, connector);
    }

    @Primary
    @Bean("distributedQueryBus")
    public QueryBus distributedQueryBus(@Qualifier("localSegment") QueryBus localQueryBus,
                                        QueryCapabilityRegistry capabilityRegistry,
                                        SpringCloudQueryRouter router,
                                        HttpQueryBusConnector connector,
                                        @Qualifier("axonKafkaQueryDispatchExecutor") Executor axonKafkaQueryDispatchExecutor) {
        return new SpringCloudDistributedQueryBus(localQueryBus, capabilityRegistry, router, connector,
                axonKafkaQueryDispatchExecutor);
    }

    @Bean
    public QueryCapabilitiesController queryCapabilitiesController(QueryCapabilityRegistry registry,
                                                                   Registration registration) {
        return new QueryCapabilitiesController(registry, registration);
    }

    @Bean
    public QueryTransportController queryTransportController(@Qualifier("localSegment") QueryBus localQueryBus,
                                                             DistributedQuerySerializer querySerializer) {
        return new QueryTransportController(localQueryBus, querySerializer.serializer());
    }

    /**
     * Détient le serializer du query bus distribué sans exposer un bean {@link Serializer}
     * supplémentaire (les serializers Axon général/message/event ne sont pas impactés).
     */
    public record DistributedQuerySerializer(Serializer serializer) {
    }
}
