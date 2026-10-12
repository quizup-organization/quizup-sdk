package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.console.QuizupDeadLetterQueueProviderConfigurerModule;
import jakarta.persistence.EntityManagerFactory;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.serialization.Serializer;
import org.axonframework.springboot.EventProcessorProperties;
import org.axonframework.springboot.util.DeadLetterQueueProviderConfigurerModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AxonConsoleDlqAutoConfigurationTest {

    @Configuration(proxyBeanMethods = false)
    static class TestSerializers {

        @Bean
        @Primary
        Serializer serializer() {
            return mock(Serializer.class);
        }

        @Bean("eventSerializer")
        Serializer eventSerializer() {
            return mock(Serializer.class);
        }
    }

    private final ApplicationContextRunner baseRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AxonConsoleDlqAutoConfiguration.class))
            .withUserConfiguration(TestSerializers.class)
            .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
            .withBean(EntityManagerProvider.class, () -> mock(EntityManagerProvider.class))
            .withBean(TransactionManager.class, () -> mock(TransactionManager.class))
            .withBean(EventProcessorProperties.class, EventProcessorProperties::new)
            .withBean(InstanceScopedProcessingGroupsProperties.class,
                    () -> new InstanceScopedProcessingGroupsProperties(List.of(), null));

    @Test
    void replacesTheAxonNoOpDlqModule() {
        baseRunner.run(context -> assertThat(context)
                .hasSingleBean(DeadLetterQueueProviderConfigurerModule.class)
                .getBean(DeadLetterQueueProviderConfigurerModule.class)
                .isInstanceOf(QuizupDeadLetterQueueProviderConfigurerModule.class));
    }

    @Test
    void backsOffWithoutEntityManagerFactory() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AxonConsoleDlqAutoConfiguration.class))
                .withUserConfiguration(TestSerializers.class)
                .withBean(EntityManagerProvider.class, () -> mock(EntityManagerProvider.class))
                .withBean(TransactionManager.class, () -> mock(TransactionManager.class))
                .withBean(EventProcessorProperties.class, EventProcessorProperties::new)
                .withBean(InstanceScopedProcessingGroupsProperties.class,
                        () -> new InstanceScopedProcessingGroupsProperties(List.of(), null))
                .run(context -> assertThat(context)
                        .doesNotHaveBean(DeadLetterQueueProviderConfigurerModule.class));
    }

    @Test
    void winsOverTheAxonJpaModuleWhenBothArePresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AxonConsoleDlqAutoConfiguration.class,
                        org.axonframework.springboot.autoconfig.JpaAutoConfiguration.class))
                .withUserConfiguration(TestSerializers.class)
                .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
                .withBean(EntityManagerProvider.class, () -> mock(EntityManagerProvider.class))
                .withBean(TransactionManager.class, () -> mock(TransactionManager.class))
                .withBean(EventProcessorProperties.class, EventProcessorProperties::new)
                .withBean(InstanceScopedProcessingGroupsProperties.class,
                        () -> new InstanceScopedProcessingGroupsProperties(List.of(), null))
                .run(context -> assertThat(context)
                        .hasSingleBean(DeadLetterQueueProviderConfigurerModule.class)
                        .getBean(DeadLetterQueueProviderConfigurerModule.class)
                        .isInstanceOf(QuizupDeadLetterQueueProviderConfigurerModule.class));
    }
}
