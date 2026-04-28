package com.managination.numa.didserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the DID Server application.
 * <p>
 * This Spring Boot application provides a REST API for managing Decentralized Identifiers (DIDs)
 * using the did:webvh method. It supports uploading, verifying, and serving DID documents.
 * </p>
 *
 * @author Swiss Federal Chancellery
 */
@SpringBootApplication
public class DidServerApplication {

    /**
     * Application entry point that bootstraps the Spring Boot context.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(DidServerApplication.class, args);
    }
}

