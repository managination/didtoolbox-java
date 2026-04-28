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

/**
 * Spring configuration class that registers Swagger UI resources and filters.
 * <p>
 * This configuration enables Swagger UI to be served from the {@code /swagger-ui} path
 * by intercepting requests and serving static resources from the Swagger UI webjars classpath.
 * </p>
 *
 * @author Swiss Federal Chancellery
 */
@Configuration
public class SwaggerUiConfig {

   /**
    * Set of static resource file paths required by Swagger UI (CSS, JS, favicons).
    * These paths are matched to determine whether a request should be handled by the
    * Swagger UI filter or passed through to the rest of the application.
    */
   private static final Set<String> SWAGGER_UI_RESOURCES = Set.of(
         "/index.css",
         "/swagger-ui.css",
         "/swagger-ui-bundle.js",
         "/swagger-ui-standalone-preset.js",
         "/swagger-initializer.js",
         "/favicon-32x32.png",
         "/favicon-16x16.png"
   );

   /**
    * Registers the {@link SwaggerUiFilter} to handle all incoming requests ({@code /*}).
    * The filter intercepts Swagger UI-related requests and serves the appropriate static
    * resources from the classpath.
    *
    * @return a {@link FilterRegistrationBean} configured with the Swagger UI filter
    */
   @Bean
   public FilterRegistrationBean<SwaggerUiFilter> swaggerUiFilterRegistration() {
      System.out.println("SwaggerUiConfig: Registering SwaggerUiFilter with pattern /*");
      FilterRegistrationBean<SwaggerUiFilter> registration = new FilterRegistrationBean<>();
      registration.setFilter(new SwaggerUiFilter());
      registration.addUrlPatterns("/*");
      registration.setOrder(0);
      return registration;
   }

   /**
    * Servlet filter that intercepts requests for Swagger UI resources and serves them
    * from the classpath-based webjars location.
    * <p>
    * Requests matching the {@code /swagger-ui} prefix or known Swagger UI resource paths
    * are handled by this filter. All other requests are passed through the filter chain.
    * </p>
    */
   public static class SwaggerUiFilter implements Filter {

      /**
       * Classpath prefix where Swagger UI static resources are located within the webjars.
       */
      private static final String SWAGGER_UI_CLASSPATH = "/META-INF/resources/webjars/swagger-ui/5.10.3/";

      private static final Logger log = LoggerFactory.getLogger(SwaggerUiFilter.class);

      /**
       * Spring resource handler configured to serve static resources from the Swagger UI classpath.
       */
      private final ResourceHttpRequestHandler handler;

      /**
       * Constructs the filter and initializes the {@link ResourceHttpRequestHandler}
       * with the Swagger UI classpath location and a {@link PathResourceResolver}.
       *
       * @throws RuntimeException if the resource handler fails to initialize
       */
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

      /**
       * Processes each request, determining whether it is a Swagger UI resource request.
       * <p>
       * If the request targets a Swagger UI resource, the filter maps the request path
       * to the appropriate classpath resource and handles it directly. Otherwise, the
       * request is passed to the next filter in the chain.
       * </p>
       *
       * @param servletRequest  the incoming servlet request
       * @param servletResponse the outgoing servlet response
       * @param chain           the filter chain for passing non-Swagger UI requests
       * @throws IOException      if an I/O error occurs during request handling
       * @throws ServletException if a servlet-specific error occurs
       */
      @Override
      public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
         HttpServletRequest request = (HttpServletRequest) servletRequest;
         HttpServletResponse response = (HttpServletResponse) servletResponse;
         String path = request.getRequestURI();

         System.out.println("SwaggerUiFilter: path=" + path);
         log.info("SwaggerUiFilter: path={}", path);

         boolean isSwaggerUiResource = path.equals("/swagger-ui") || path.equals("/swagger-ui/index.html") ||
               path.startsWith("/swagger-ui/") || SWAGGER_UI_RESOURCES.contains(path);

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
