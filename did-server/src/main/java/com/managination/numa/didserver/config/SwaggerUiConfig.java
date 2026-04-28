package com.managination.numa.didserver.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Configuration
public class SwaggerUiConfig {

    private static final Set<String> SWAGGER_UI_RESOURCES = Set.of(
        "/index.css",
        "/swagger-ui.css",
        "/swagger-ui-bundle.js",
        "/swagger-ui-standalone-preset.js",
        "/swagger-initializer.js",
        "/favicon-32x32.png",
        "/favicon-16x16.png"
    );

    @Bean
    public FilterRegistrationBean<SwaggerUiFilter> swaggerUiFilterRegistration() {
        System.out.println("SwaggerUiConfig: Registering SwaggerUiFilter with pattern /*");
        FilterRegistrationBean<SwaggerUiFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SwaggerUiFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(0);
        return registration;
    }

    public static class SwaggerUiFilter implements Filter {

        private static final String SWAGGER_UI_CLASSPATH = "/META-INF/resources/webjars/swagger-ui/5.10.3/";
        private static final Logger log = LoggerFactory.getLogger(SwaggerUiFilter.class);

        private final ResourceHttpRequestHandler handler;

        public SwaggerUiFilter() {
            System.out.println("SwaggerUiFilter: Constructor called");
            log.info("SwaggerUiFilter: Constructor called");
            this.handler = new ResourceHttpRequestHandler();
            this.handler.setLocations(List.of(new ClassPathResource(SWAGGER_UI_CLASSPATH)));
            this.handler.setResourceResolvers(List.of(new PathResourceResolver()));
            try {
                this.handler.afterPropertiesSet();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize SwaggerUiResourceHttpRequestHandler", e);
            }
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            String path = request.getRequestURI();

            System.out.println("SwaggerUiFilter: path=" + path);
            log.info("SwaggerUiFilter: path={}", path);

            boolean isSwaggerUiResource = path.equals("/swagger-ui") || path.equals("/swagger-ui/") ||
                path.equals("/swagger-ui/index.html") || path.startsWith("/swagger-ui/") ||
                SWAGGER_UI_RESOURCES.contains(path);

            System.out.println("SwaggerUiFilter: isSwaggerUiResource=" + isSwaggerUiResource);
            log.info("SwaggerUiFilter: isSwaggerUiResource={}", isSwaggerUiResource);

            if (isSwaggerUiResource) {
                String pathWithinHandlerMapping;
                if (path.equals("/swagger-ui") || path.equals("/swagger-ui/") || path.equals("/swagger-ui/index.html")) {
                    pathWithinHandlerMapping = "index.html";
                } else if (path.startsWith("/swagger-ui/")) {
                    pathWithinHandlerMapping = path.substring("/swagger-ui/".length());
                } else {
                    pathWithinHandlerMapping = path.substring(1);
                }
                System.out.println("SwaggerUiFilter: handling pathWithinHandlerMapping=" + pathWithinHandlerMapping);
                log.info("SwaggerUiFilter: pathWithinHandlerMapping={}", pathWithinHandlerMapping);
                request.setAttribute("org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping", pathWithinHandlerMapping);
                request.setAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingHandler", handler);
                request.setAttribute("org.springframework.web.servlet.HandlerMapping.lookupHandlerLookup", path);
                handler.handleRequest(request, response);
                return;
            }
            chain.doFilter(servletRequest, servletResponse);
        }
    }
}
