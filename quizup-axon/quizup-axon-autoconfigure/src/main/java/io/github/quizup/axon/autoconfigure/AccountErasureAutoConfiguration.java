package io.github.quizup.axon.autoconfigure;

import io.github.quizup.axon.autoconfigure.erasure.AxonEventStoreErasureService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Outil d'effacement physique de l'event store pour les suppressions de compte. Disponible
 * dans tous les services qui ont un {@link DataSource} et spring-jdbc.
 */
@AutoConfiguration
@AutoConfigureAfter(org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class)
@ConditionalOnClass(JdbcTemplate.class)
public class AccountErasureAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public AxonEventStoreErasureService axonEventStoreErasureService(DataSource dataSource) {
        return new AxonEventStoreErasureService(dataSource);
    }
}
