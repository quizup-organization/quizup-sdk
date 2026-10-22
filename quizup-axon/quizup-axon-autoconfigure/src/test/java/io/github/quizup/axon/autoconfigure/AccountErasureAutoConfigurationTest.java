package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.autoconfigure.erasure.AxonEventStoreErasureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AccountErasureAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AccountErasureAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class));

    @Test
    void registerErasureServiceWhenDataSourceIsAvailable() {
        runner.run(context ->
                assertThat(context).hasSingleBean(AxonEventStoreErasureService.class));
    }
}
