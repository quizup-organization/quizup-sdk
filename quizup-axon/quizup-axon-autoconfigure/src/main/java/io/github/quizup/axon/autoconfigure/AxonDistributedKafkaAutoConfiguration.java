package io.github.quizup.axon.autoconfigure;

import org.apache.kafka.clients.admin.NewTopic;
import org.axonframework.config.ConfigurerModule;
import org.axonframework.extensions.kafka.KafkaProperties;
import org.axonframework.springboot.autoconfig.AxonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@AutoConfigureBefore(AxonAutoConfiguration.class)
public class AxonDistributedKafkaAutoConfiguration {

    @Bean
    public ConfigurerModule axonDefaultProcessingGroupConfigurerModule(Environment environment) {
        String appName = environment.getProperty("spring.application.name", "app-inconnue");

        return configurer -> configurer.eventProcessing(eventProcessing ->
                eventProcessing
                        .byDefaultAssignHandlerInstancesTo(handler -> resolveGroup(handler.getClass(), appName))
                        .byDefaultAssignHandlerTypesTo(handlerType -> resolveGroup(handlerType, appName))
        );
    }

    private static String resolveGroup(Class<?> handlerClass, String appName) {
        boolean isSaga = handlerClass.isAnnotationPresent(org.axonframework.spring.stereotype.Saga.class);
        return isSaga ? appName + "-saga" : appName + "-projection";
    }

    @Bean
    public NewTopic axonDefaultEventTopic(Environment environment) {
        String topicName = environment.getProperty("axon.kafka.default-topic", KafkaProperties.DEFAULT_TOPIC);
        return new NewTopic(topicName, 3, (short) 1);
    }
}