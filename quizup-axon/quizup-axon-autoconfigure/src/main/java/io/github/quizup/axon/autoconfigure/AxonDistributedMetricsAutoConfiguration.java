package io.github.quizup.axon.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.axonframework.micrometer.GlobalMetricRegistry;
import org.axonframework.micrometer.MetricsConfigurerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Branche les métriques Axon (messages dispatchés/traités/en échec, latence des bus, event
 * processors) sur le {@link MeterRegistry} Micrometer exposé par Actuator.
 * <p>
 * Activé dès qu'un {@code MeterRegistry} est disponible (registre Prometheus apporté par le
 * starter). Sur les tests Axon in-memory (sans Actuator), la configuration est ignorée.
 */
@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class, GlobalMetricRegistry.class})
@ConditionalOnBean(MeterRegistry.class)
@AutoConfigureAfter(name = {
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration"
})
public class AxonDistributedMetricsAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AxonDistributedMetricsAutoConfiguration.class);

    public AxonDistributedMetricsAutoConfiguration() {
        logger.info("Axon metrics auto-configuration enabled");
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalMetricRegistry globalMetricRegistry(MeterRegistry meterRegistry) {
        return new GlobalMetricRegistry(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsConfigurerModule axonMetricsConfigurerModule(GlobalMetricRegistry globalMetricRegistry) {
        return new MetricsConfigurerModule(globalMetricRegistry);
    }
}
