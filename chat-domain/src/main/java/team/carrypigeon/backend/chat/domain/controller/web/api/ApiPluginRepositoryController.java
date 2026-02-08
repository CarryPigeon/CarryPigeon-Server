package team.carrypigeon.backend.chat.domain.controller.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.chat.domain.service.catalog.ApiPluginCatalogIndex;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Public endpoints for plugin package repository (download + domain contract schema).
 * <p>
 * These endpoints are used by clients during required gate onboarding.
 */
@RestController
public class ApiPluginRepositoryController {

    private final ApiPluginCatalogIndex catalogIndex;

    public ApiPluginRepositoryController(ApiPluginCatalogIndex catalogIndex) {
        this.catalogIndex = catalogIndex;
    }

    /**
     * Download a plugin package zip.
     * <p>
     * Route: {@code GET /api/plugins/download/{plugin_id}/{version}}
     */
    @GetMapping("/api/plugins/download/{plugin_id}/{version}")
    public ResponseEntity<InputStreamResource> download(@PathVariable("plugin_id") String pluginId,
                                                        @PathVariable("version") String version) throws Exception {
        ApiPluginCatalogIndex.PluginPackageFile f = catalogIndex.snapshot().pluginFile(pluginId, version);
        if (f == null || f.path() == null) {
            throw new CPProblemException(CPProblem.of(404, "not_found", "plugin not found"));
        }
        Path path = f.path();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new CPProblemException(CPProblem.of(404, "not_found", "plugin not found"));
        }
        InputStream in = Files.newInputStream(path);
        InputStreamResource resource = new InputStreamResource(in);
        String filename = (pluginId + "-" + version + ".zip").replaceAll("[\\r\\n\\\"]", "_");
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("x-cp-sha256", f.sha256() == null ? "" : f.sha256())
                .body(resource);
    }

    /**
     * Get JSON schema for a domain contract.
     * <p>
     * Route: {@code GET /api/contracts/{plugin_id}/{domain}/{domain_version}}
     */
    @GetMapping("/api/contracts/{plugin_id}/{domain}/{domain_version}")
    public JsonNode contract(@PathVariable("plugin_id") String pluginId,
                             @PathVariable("domain") String domain,
                             @PathVariable("domain_version") String domainVersion) {
        ApiPluginCatalogIndex.ContractFile f = catalogIndex.snapshot().contractFile(pluginId, domain, domainVersion);
        if (f == null || f.schema() == null) {
            throw new CPProblemException(CPProblem.of(404, "not_found", "contract not found"));
        }
        // Schema is public and small; return directly.
        return f.schema();
    }
}
