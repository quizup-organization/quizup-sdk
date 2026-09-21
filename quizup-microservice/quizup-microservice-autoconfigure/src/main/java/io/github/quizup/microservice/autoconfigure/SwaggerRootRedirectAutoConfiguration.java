package io.github.quizup.microservice.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@AutoConfiguration
@ConditionalOnProperty(prefix = "microservice.swagger", name = "use-root-path", havingValue = "true", matchIfMissing = true)
public class SwaggerRootRedirectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RootRedirectController rootRedirectController() {
        return new RootRedirectController();
    }

    @Controller
    public static class RootRedirectController {

        @GetMapping({"", "/"})
        public String redirectToSwagger() {
            return "redirect:/swagger-ui/index.html";
        }
    }
}