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
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.chat.domain.service.catalog.ApiPluginCatalogIndex;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 插件仓库 API 控制器。
 * <p>
 * 提供插件包下载与 domain 合约查询路由。
 */
@RestController
public class ApiPluginRepositoryController {

    private final ApiPluginCatalogIndex catalogIndex;

    /**
     * 构造插件仓库控制器。
     *
     * @param catalogIndex 插件目录索引服务。
     */
    public ApiPluginRepositoryController(ApiPluginCatalogIndex catalogIndex) {
        this.catalogIndex = catalogIndex;
    }

    /**
     * 下载指定插件版本的压缩包。
     *
     * @param pluginId 插件 ID。
     * @param version 插件版本。
     * @return 插件包下载响应。
     * @throws Exception 当读取插件包文件失败时抛出。
     */
    @GetMapping("/api/plugins/download/{plugin_id}/{version}")
    public ResponseEntity<InputStreamResource> download(@PathVariable("plugin_id") String pluginId,
                                                        @PathVariable("version") String version) throws Exception {
        ApiPluginCatalogIndex.PluginPackageFile f = catalogIndex.snapshot().pluginFile(pluginId, version);
        if (f == null || f.path() == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.NOT_FOUND, "plugin not found"));
        }
        Path path = f.path();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.NOT_FOUND, "plugin not found"));
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
     * 查询插件提供的 domain 合约 Schema。
     *
     * @param pluginId 插件 ID。
     * @param domain domain 名称。
     * @param domainVersion domain 版本。
     * @return JSON Schema 内容。
     */
    @GetMapping("/api/contracts/{plugin_id}/{domain}/{domain_version}")
    public JsonNode contract(@PathVariable("plugin_id") String pluginId,
                             @PathVariable("domain") String domain,
                             @PathVariable("domain_version") String domainVersion) {
        ApiPluginCatalogIndex.ContractFile f = catalogIndex.snapshot().contractFile(pluginId, domain, domainVersion);
        if (f == null || f.schema() == null) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.NOT_FOUND, "contract not found"));
        }
        return f.schema();
    }
}
