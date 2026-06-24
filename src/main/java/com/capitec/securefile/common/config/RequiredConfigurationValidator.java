package com.capitec.securefile.common.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.PlaceholderResolutionException;

import java.util.List;

@Component
public class RequiredConfigurationValidator implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final List<String> REQUIRED_PROPERTIES = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "securefile.jwt.secret",
            "securefile.download-link.secret",
            "securefile.s3.endpoint",
            "securefile.s3.region",
            "securefile.s3.bucket",
            "securefile.s3.access-key",
            "securefile.s3.secret-key");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<String> missingProperties = REQUIRED_PROPERTIES.stream()
                .filter(this::isMissing)
                .toList();
        if (!missingProperties.isEmpty()) {
            throw new IllegalStateException("Missing required configuration: " + String.join(", ", missingProperties));
        }
    }

    private boolean isMissing(String propertyName) {
        String value;
        try {
            value = environment.getProperty(propertyName);
        } catch (PlaceholderResolutionException ex) {
            return true;
        }
        return value == null || value.isBlank() || isUnresolvedPlaceholder(value);
    }

    private boolean isUnresolvedPlaceholder(String value) {
        return value.startsWith("${") && value.endsWith("}");
    }
}
