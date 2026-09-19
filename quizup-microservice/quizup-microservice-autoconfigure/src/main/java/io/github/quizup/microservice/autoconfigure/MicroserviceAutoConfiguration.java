package io.github.quizup.microservice.autoconfigure;

import io.github.quizup.microservice.MicroserviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(MicroserviceProperties.class)
public class MicroserviceAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceAutoConfiguration.class);

    public MicroserviceAutoConfiguration() {
        logger.info("MicroserviceAutoConfiguration enabled");
    }
}
