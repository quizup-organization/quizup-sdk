package io.github.quizup.axon.autoconfigure;

import org.axonframework.config.Configurer;
import org.axonframework.config.Configuration;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.messaging.StreamableMessageSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstanceScopedProcessingGroupsAutoConfigurationTest {

    @Test
    void instanceScopedGroup_runsUnderItsOwnSuffixedProcessor() {
        StreamableMessageSource<TrackedEventMessage<?>> source = mockKafkaSource();
        Configurer configurer = baseConfigurer(source);

        new InstanceScopedProcessingGroupsAutoConfiguration()
                .instanceScopedProcessingGroupsConfigurer(
                        new InstanceScopedProcessingGroupsProperties(List.of("fanout"), "pod-1"),
                        new MockEnvironment(),
                        applicationContext(source))
                .configureModule(configurer);

        Configuration configuration = configurer.buildConfiguration();

        assertThat(configuration.eventProcessingConfiguration().eventProcessors())
                .containsKey("fanout-pod-1")
                .doesNotContainKey("fanout");
    }

    @Test
    void withoutInstanceScopedGroups_theAnnotatedGroupIsUntouched() {
        StreamableMessageSource<TrackedEventMessage<?>> source = mockKafkaSource();
        Configurer configurer = baseConfigurer(source);

        new InstanceScopedProcessingGroupsAutoConfiguration()
                .instanceScopedProcessingGroupsConfigurer(
                        new InstanceScopedProcessingGroupsProperties(List.of(), null),
                        new MockEnvironment(),
                        applicationContext(source))
                .configureModule(configurer);

        Configuration configuration = configurer.buildConfiguration();

        assertThat(configuration.eventProcessingConfiguration().eventProcessors())
                .containsKey("fanout");
    }

    /**
     * Garde-fou anti-replay : un groupe instance-scopé démarre à la <b>queue</b> du flux (head).
     * {@code createTailToken()} renvoie {@code null} pour Kafka et ferait rejouer tout le topic à
     * chaque nouveau pod (notifications « déjà reçues » re-poussées aux clients connectés).
     */
    @Test
    void initialToken_startsAtHead_soANewPodDoesNotReplayTheStream() {
        StreamableMessageSource<TrackedEventMessage<?>> source = mockKafkaSource();
        TrackingToken head = mock(TrackingToken.class);
        when(source.createHeadToken()).thenReturn(head);

        TrackingToken token = InstanceScopedProcessingGroupsAutoConfiguration
                .initialTokenBuilder()
                .apply(source);

        assertThat(token).isSameAs(head);
    }

    @Test
    void instanceId_fallsBackToHostnameEnvironment() {
        StreamableMessageSource<TrackedEventMessage<?>> source = mockKafkaSource();
        Configurer configurer = baseConfigurer(source);
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("HOSTNAME", "pod-42");

        new InstanceScopedProcessingGroupsAutoConfiguration()
                .instanceScopedProcessingGroupsConfigurer(
                        new InstanceScopedProcessingGroupsProperties(List.of("fanout"), null),
                        environment,
                        applicationContext(source))
                .configureModule(configurer);

        Configuration configuration = configurer.buildConfiguration();

        assertThat(configuration.eventProcessingConfiguration().eventProcessors())
                .containsKey("fanout-pod-42");
    }

    @SuppressWarnings("unchecked")
    private static StreamableMessageSource<TrackedEventMessage<?>> mockKafkaSource() {
        return mock(StreamableMessageSource.class);
    }

    private static Configurer baseConfigurer(StreamableMessageSource<TrackedEventMessage<?>> source) {
        Configurer configurer = DefaultConfigurer.defaultConfiguration();
        configurer.eventProcessing()
                .configureDefaultStreamableMessageSource(configuration -> source)
                .registerEventHandler(configuration -> new FanoutHandler());
        return configurer;
    }

    private static ApplicationContext applicationContext(
            StreamableMessageSource<TrackedEventMessage<?>> source) {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString(), eq(StreamableMessageSource.class)))
                .thenReturn(source);
        return applicationContext;
    }

    @ProcessingGroup("fanout")
    static class FanoutHandler {

        @EventHandler
        public void on(FanoutEvent event) {
            // Handler de test : seul le rattachement au groupe compte.
        }
    }

    record FanoutEvent(String id) {
    }
}
