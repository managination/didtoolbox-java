package com.managination.numa.didserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC configuration for the DID Server application.
 * <p>
 * This configuration class registers view controllers that handle URL redirects,
 * specifically redirecting {@code /swagger-ui} and {@code /swagger-ui.html} to
 * the canonical {@code /swagger-ui/} path for consistent Swagger UI access.
 * </p>
 *
 * @author Swiss Federal Chancellery
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registers view controllers for URL redirects.
     * <p>
     * The following redirects are configured:
     * <ul>
     *   <li>{@code /swagger-ui} &rarr; {@code /swagger-ui/}</li>
     *   <li>{@code /swagger-ui.html} &rarr; {@code /swagger-ui/}</li>
     * </ul>
     * This ensures that all Swagger UI entry points resolve to the same location.
     * </p>
     *
     * @param registry the {@link ViewControllerRegistry} to register redirects with
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/swagger-ui", "/swagger-ui/");
        registry.addRedirectViewController("/swagger-ui.html", "/swagger-ui/");
    }
}
