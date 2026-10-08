package io.github.quizup.microservice.autoconfigure;

import io.github.quizup.microservice.MicroserviceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le wiring STOMP : broker simple, heartbeats par défaut (10 s) et désactivation à zéro.
 */
class WebSocketAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebSocketAutoConfiguration.class))
            .withUserConfiguration(Config.class);

    @Test
    void configuresBrokerHeartbeatsByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            SimpleBrokerMessageHandler broker = context.getBean(SimpleBrokerMessageHandler.class);

            assertThat(broker.getHeartbeatValue()).containsExactly(10_000L, 10_000L);
            assertThat(broker.getTaskScheduler()).isNotNull();
        });
    }

    @Test
    void heartbeatsAreDisabledWhenBothIntervalsAreZero() {
        runner.withPropertyValues(
                        "microservice.websocket.heartbeat-outgoing=0s",
                        "microservice.websocket.heartbeat-incoming=0s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SimpleBrokerMessageHandler broker = context.getBean(SimpleBrokerMessageHandler.class);

                    assertThat(broker.getHeartbeatValue()).isNull();
                    assertThat(broker.getTaskScheduler()).isNull();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MicroserviceProperties.class)
    static class Config {
    }
}
