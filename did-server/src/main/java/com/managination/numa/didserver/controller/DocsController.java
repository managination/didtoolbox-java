package com.managination.numa.didserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller for serving API documentation.
 * <p>
 * Provides two endpoints under {@code /docs}:
 * <ul>
 *   <li>{@code GET /docs/openapi.json} - Returns the raw OpenAPI 3.0 specification in JSON format</li>
 *   <li>{@code GET /docs} - Returns an interactive HTML documentation page</li>
 * </ul>
 * </p>
 *
 * @author Swiss Federal Chancellery
 */
@RestController
@RequestMapping("/docs")
@Tag(name = "Documentation", description = "API documentation endpoints")
public class DocsController {

    /**
     * Classpath location of the OpenAPI specification JSON file.
     */
    private static final String OPENAPI_JSON_PATH = "openapi.json";

    /**
     * Retrieves the OpenAPI 3.0 specification for the DID Server API.
     * <p>
     * The specification is read from the classpath resource {@code openapi.json}
     * and returned as a UTF-8 encoded JSON string.
     * </p>
     *
     * @return a {@link ResponseEntity} containing the OpenAPI JSON specification
     * @throws IOException if the classpath resource cannot be read
     */
    @Operation(
        summary = "Get OpenAPI specification",
        description = "Returns the OpenAPI 3.0 specification for the DID Server API in JSON format"
    )
    @GetMapping(value = "/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOpenApiSpec() throws IOException {
        ClassPathResource resource = new ClassPathResource(OPENAPI_JSON_PATH);
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(content);
    }

    /**
     * Returns an HTML page with interactive documentation for the DID Server API.
     * <p>
     * The page includes an overview of available endpoints, DID format specifications,
     * error handling details, and navigation links to Swagger UI and the raw OpenAPI JSON.
     * </p>
     *
     * @return a {@link ResponseEntity} containing the HTML documentation page
     */
    @Operation(
        summary = "Get API documentation (HTML)",
        description = "Returns an HTML page with interactive documentation for the DID Server API"
    )
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getDocumentation() {
        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>DID Server API Documentation</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                        background: #f5f5f5;
                        color: #333;
                        line-height: 1.6;
                    }
                    .header {
                        background: linear-gradient(135deg, #1a365d 0%, #2c5282 100%);
                        color: white;
                        padding: 2rem;
                        text-align: center;
                    }
                    .header h1 { font-size: 2rem; margin-bottom: 0.5rem; }
                    .header p { opacity: 0.9; font-size: 1.1rem; }
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                        padding: 2rem;
                    }
                    .card {
                        background: white;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        padding: 1.5rem;
                        margin-bottom: 1.5rem;
                    }
                    .card h2 {
                        color: #1a365d;
                        font-size: 1.3rem;
                        margin-bottom: 1rem;
                        padding-bottom: 0.5rem;
                        border-bottom: 2px solid #e2e8f0;
                    }
                    .endpoint {
                        background: #f8fafc;
                        border-left: 4px solid #2c5282;
                        padding: 1rem;
                        margin-bottom: 1rem;
                        border-radius: 0 4px 4px 0;
                    }
                    .endpoint:hover { background: #edf2f7; }
                    .method {
                        display: inline-block;
                        padding: 0.25rem 0.75rem;
                        border-radius: 4px;
                        font-weight: bold;
                        font-size: 0.85rem;
                        margin-right: 0.5rem;
                    }
                    .method.get { background: #48bb78; color: white; }
                    .method.post { background: #3182ce; color: white; }
                    .method.put { background: #ed8936; color: white; }
                    .method.delete { background: #e53e3e; color: white; }
                    .path {
                        font-family: 'Monaco', 'Menlo', monospace;
                        font-size: 0.95rem;
                        color: #2d3748;
                    }
                    .description { margin-top: 0.5rem; color: #4a5568; }
                    .badge {
                        display: inline-block;
                        padding: 0.2rem 0.5rem;
                        background: #edf2f7;
                        border-radius: 4px;
                        font-size: 0.75rem;
                        color: #4a5568;
                        margin-top: 0.5rem;
                    }
                    .nav-links {
                        display: flex;
                        gap: 1rem;
                        margin-top: 1rem;
                    }
                    .nav-links a {
                        display: inline-block;
                        padding: 0.75rem 1.5rem;
                        background: #2c5282;
                        color: white;
                        text-decoration: none;
                        border-radius: 4px;
                        font-weight: 500;
                        transition: background 0.2s;
                    }
                    .nav-links a:hover { background: #1a365d; }
                    .info-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 1rem;
                        margin-top: 1rem;
                    }
                    .info-item {
                        background: #f8fafc;
                        padding: 1rem;
                        border-radius: 4px;
                    }
                    .info-item strong { color: #1a365d; }
                    code {
                        background: #edf2f7;
                        padding: 0.2rem 0.4rem;
                        border-radius: 3px;
                        font-size: 0.9em;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>DID Server API</h1>
                    <p>Decentralized Identifier (DID) Management API using did:webvh method</p>
                    <div class="nav-links">
                        <a href="/docs">Interactive Docs</a>
                        <a href="/swagger-ui">Swagger UI</a>
                        <a href="/v3/api-docs">OpenAPI JSON</a>
                    </div>
                </div>
                <div class="container">
                    <div class="card">
                        <h2>API Overview</h2>
                        <div class="info-grid">
                            <div class="info-item">
                                <strong>Base URL</strong><br>
                                <code>http://localhost:8080</code>
                            </div>
                            <div class="info-item">
                                <strong>API Version</strong><br>
                                <code>1.9.1-SNAPSHOT</code>
                            </div>
                            <div class="info-item">
                                <strong>Authentication</strong><br>
                                None (public API)
                            </div>
                            <div class="info-item">
                                <strong>Content Type</strong><br>
                                <code>application/json</code>
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h2>Endpoints</h2>

                        <div class="endpoint">
                            <span class="method get">GET</span>
                            <span class="path">/health</span>
                            <p class="description">
                                Check server health status including filesystem, memory, JVM, and environment checks.
                                Returns 503 if any component is degraded.
                            </p>
                            <span class="badge">Returns 200 OK or 503 Service Unavailable</span>
                        </div>

                        <div class="endpoint">
                            <span class="method post">POST</span>
                            <span class="path">/dids</span>
                            <p class="description">
                                Upload and verify a DID log file in JSONL format. The domain in the DID ID must match
                                the Host header. If the file already exists, the SCID must match.
                            </p>
                            <p class="description" style="margin-top: 0.5rem;">
                                <strong>Content-Type:</strong> <code>multipart/form-data</code><br>
                                <strong>Form Field:</strong> <code>didJsonl</code> (required)
                            </p>
                            <span class="badge">Returns 200 OK on success, 400 Bad Request on validation failure</span>
                        </div>

                        <div class="endpoint">
                            <span class="method get">GET</span>
                            <span class="path">/**</span>
                            <p class="description">
                                Download a DID document. The DID is resolved from the storage path using the
                                Host header for domain and the request path for sub-path resolution.
                            </p>
                            <p class="description" style="margin-top: 0.5rem;">
                                <strong>Required Header:</strong> <code>Host</code> (e.g., <code>example.com</code>)
                            </p>
                            <p class="description">
                                Files are served from: <code>{storage.path}/{domain}/{path}/did.jsonl</code>
                            </p>
                            <span class="badge">Returns 200 OK, 404 Not Found, 405 Method Not Allowed, or 500 Internal Server Error</span>
                        </div>
                    </div>

                    <div class="card">
                        <h2>DID Format</h2>
                        <p>This server supports the <code>did:webvh</code> method with SCID (Self-Contained Identifier).</p>
                        <p class="description" style="margin-top: 1rem;">
                            <strong>Format:</strong> <code>did:webvh:SCID:domain[:path...]</code>
                        </p>
                        <p class="description" style="margin-top: 0.5rem;">
                            <strong>Example:</strong> <code>did:webvh:SCID:example.com:.well-known</code>
                        </p>
                        <p class="description" style="margin-top: 1rem;">
                            DID files are stored as <code>did.jsonl</code> in the following structure:
                        </p>
                        <ul style="margin-top: 0.5rem; margin-left: 1.5rem;">
                            <li><code>{storagePath}/{domain}/.well-known/did.jsonl</code> (root domain)</li>
                            <li><code>{storagePath}/{domain}/{path}/did.jsonl</code> (sub-path)</li>
                        </ul>
                    </div>

                    <div class="card">
                        <h2>Error Handling</h2>
                        <div class="endpoint" style="margin-bottom: 0.5rem;">
                            <span class="path">400 Bad Request</span>
                            <p class="description">Validation failed (empty file, domain mismatch, SCID mismatch, or verification failure)</p>
                        </div>
                        <div class="endpoint" style="margin-bottom: 0.5rem;">
                            <span class="path">404 Not Found</span>
                            <p class="description">DID document not found at the specified path</p>
                        </div>
                        <div class="endpoint" style="margin-bottom: 0.5rem;">
                            <span class="path">405 Method Not Allowed</span>
                            <p class="description">Invalid method for the requested path (e.g., POST to /health)</p>
                        </div>
                        <div class="endpoint">
                            <span class="path">500 Internal Server Error</span>
                            <p class="description">Unexpected error during DID resolution or file serving</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """;
        return ResponseEntity.ok(html);
    }
}
