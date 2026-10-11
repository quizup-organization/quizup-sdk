package io.github.quizup.axon.autoconfigure;

import org.axonframework.config.Configurer;
import org.axonframework.config.Configuration;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.messaging.StreamableMessageSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InstanceScopedProcessingGroupsAutoConfigurationTest {

    @Test
    void instanceScopedGroup_runsUnderItsOwnSuffixedProcessor() {
        Configurer configurer = baseConfigurer();

        new InstanceScopedProcessingGroupsAutoConfiguration()
                .instanceScopedProcessingGroupsConfigurer(
                        new InstanceScopedProcessingGroupsProperties(List.of("fanout"), "pod-1"),
                        new MockEnvironment())
                .configureModule(configurer);

        Configuration configuration = configurer.buildConfiguration();

        assertThat(configuration.eventProcessingConfiguration().eventProcessors())
                .containsKey("fanout-pod-1")
                .doesNotContainKey("fanout");
    }

    @Test
    void withoutInstanceScopedGroups_theAnnotatedGroupIsUntouched() {
        Configurer configurer = baseConfigurer();

        new InstanceScopedProcessingGroupsAutoConfiguration()
                .instanceScopedProcessingGroupsConfigurer(
                        new InstanceScopedProcessingGroupsProperties(List.of(), null),
                        new MockEnvironment())
                .configureModule(configurer);

        Configuration configuration = configurer.buildConfiguration();

        assertThat(configuration.eventProcessingConfiguration().eventProcessors())
                .containsKey("fanout");
    }

    @Test
    void instanceId_fallsBackToHostnameEnvironment() {
        Configurer configurer = baseConfigurer();
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("HOSTNAME", "pod-42");

        new InstanceScopedProcessingGroupsAutoConfiguration()
                .instanceScopedProcessingGroupsConfigurer(
                        new InstanceScopedProcessingGroupsProperties(List.of("fanout"), null),
                        environment)
                .configureModule(configurer);

        Configuration configuration = configurer.buildConfiguration();

        assertThat(configuration.eventProcessingConfiguration().eventProcessors())
                .containsKey("fanout-pod-42");
    }

    private static Configurer baseConfigurer() {
        StreamableMessageSource<TrackedEventMessage<?>> source = mock(StreamableMessageSource.class);
        Configurer configurer = DefaultConfigurer.defaultConfiguration();
        configurer.eventProcessing()
                .configureDefaultStreamableMessageSource(configuration -> source)
                .registerEventHandler(configuration -> new FanoutHandler());
        return configurer;
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
