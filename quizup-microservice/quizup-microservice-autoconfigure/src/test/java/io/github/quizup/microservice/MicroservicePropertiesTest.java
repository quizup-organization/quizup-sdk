package io.github.quizup.microservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le binding des records {@link MicroserviceProperties} : défauts complets sans
 * aucune propriété `microservice.*`, et surcharge par clé.
 */
class MicroservicePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Test
    void bindsFullDefaults() {
        runner.run(context -> {
            MicroserviceProperties p = context.getBean(MicroserviceProperties.class);

            assertThat(p.cors().enabled()).isTrue();
            assertThat(p.cors().allowedOrigins()).containsExactly("*");
            assertThat(p.cors().allowedMethods()).contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            assertThat(p.cors().allowCredentials()).isTrue();
            assertThat(p.cors().maxAge()).isEqualTo(3600L);
            assertThat(p.cors().exposedHeaders()).isEmpty();

            assertThat(p.swagger().enabled()).isTrue();
            assertThat(p.swagger().version()).isEqualTo("1.0.0");
            assertThat(p.swagger().description()).isEqualTo("API Documentation");
            assertThat(p.swagger().useRootPath()).isTrue();
            assertThat(p.swagger().oauth2().enabled()).isTrue();
            assertThat(p.swagger().oauth2().clientId()).isEqualTo("swagger");
            assertThat(p.swagger().oauth2().scopes()).containsExactly("openid", "profile");
            assertThat(p.swagger().oauth2().usePkce()).isTrue();
            assertThat(p.swagger().contact().name()).isEqualTo("QuizUp Team");

            assertThat(p.exceptionHandler().enabled()).isTrue();
            assertThat(p.exceptionHandler().logStackTrace()).isTrue();
            assertThat(p.exceptionHandler().includeBindingErrors()).isTrue();

            assertThat(p.actuator().enabled()).isTrue();

            assertThat(p.resourceServer().enabled()).isTrue();
            assertThat(p.resourceServer().jwt().issuerUri()).isEqualTo("http://localhost:8085");
            assertThat(p.resourceServer().jwt().jwkSetUri()).isEqualTo("http://localhost:8085/oauth2/jwks");

            assertThat(p.websocket().enabled()).isTrue();
            assertThat(p.websocket().endpoint()).isEqualTo("/ws");
            assertThat(p.websocket().applicationDestinationPrefix()).isEqualTo("/app");
            assertThat(p.websocket().brokerDestinations()).containsExactly("/topic", "/queue");
            assertThat(p.websocket().allowedOriginPatterns()).containsExactly("*");
            assertThat(p.websocket().withSockJs()).isTrue();
            assertThat(p.websocket().requireAuth()).isFalse();
        });
    }

    @Test
    void explicitValuesOverrideDefaults() {
        runner.withPropertyValues(
                        "microservice.cors.allowed-origins=https://a.example,https://b.example",
                        "microservice.websocket.enabled=false",
                        "microservice.resource-server.jwt.jwk-set-uri=http://identity/oauth2/jwks")
                .run(context -> {
                    MicroserviceProperties p = context.getBean(MicroserviceProperties.class);

                    assertThat(p.cors().allowedOrigins())
                            .containsExactly("https://a.example", "https://b.example");
                    assertThat(p.websocket().enabled()).isFalse();
                    assertThat(p.resourceServer().jwt().jwkSetUri()).isEqualTo("http://identity/oauth2/jwks");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MicroserviceProperties.class)
    static class Config {
    }
}
