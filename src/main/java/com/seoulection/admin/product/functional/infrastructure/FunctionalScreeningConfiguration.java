package com.seoulection.admin.product.functional.infrastructure;

import com.seoulection.admin.product.functional.application.FunctionalScreeningProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FunctionalScreeningProperties.class)
public class FunctionalScreeningConfiguration {
}
