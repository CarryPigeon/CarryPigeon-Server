package team.carrypigeon.backend.chat.domain.cmp.api.server;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.service.catalog.ApiPluginCatalogIndex;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 消息域能力目录结果节点。
 * <p>
 * 生成 `GET /api/domains/catalog` 的标准响应，返回各消息域版本与约束信息。
 */
@Slf4j
@LiteflowComponent("ApiDomainsCatalogResult")
public class ApiDomainsCatalogResultNode extends AbstractResultNode<ApiDomainsCatalogResultNode.DomainsCatalogResponse> {

    private final ApiPluginCatalogIndex catalogIndex;

    /**
     * 构造函数注入目录索引服务。
     */
    public ApiDomainsCatalogResultNode(ApiPluginCatalogIndex catalogIndex) {
        this.catalogIndex = catalogIndex;
    }

    /**
     * 构建域目录响应。
     */
    @Override
    protected DomainsCatalogResponse build(CPFlowContext context) {
        DomainCatalogItem coreText = coreText();
        List<DomainCatalogItem> merged = new java.util.ArrayList<>();
        merged.add(coreText);
        for (ApiPluginCatalogIndex.DomainItem d : catalogIndex.snapshot().domainsView()) {
            if (d != null) {
                merged.add(mapScanned(d));
            }
        }
        DomainsCatalogResponse response = new DomainsCatalogResponse(merged);
        log.debug("ApiDomainsCatalogResult success");
        return response;
    }

    /**
     * 生成内置 Core:Text 域定义。
     */
    private DomainCatalogItem coreText() {
        return new DomainCatalogItem(
                "Core:Text",
                List.of("1.0.0"),
                "1.0.0",
                Map.of("max_payload_bytes", 4096, "max_depth", 10),
                List.of(Map.of("type", "core")),
                null
        );
    }

    /**
     * 将扫描得到的域项映射为 API 响应项。
     */
    private DomainCatalogItem mapScanned(ApiPluginCatalogIndex.DomainItem item) {
        Map<String, Object> constraints = Map.of(
                "max_payload_bytes", item.constraints() == null ? null : item.constraints().maxPayloadBytes(),
                "max_depth", item.constraints() == null ? null : item.constraints().maxDepth()
        );
        constraints = constraints.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<Map<String, Object>> providers = item.providers() == null ? List.of() : item.providers().stream()
                .filter(Objects::nonNull)
                .map(p -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("type", p.type());
                    if (p.pluginId() != null && !p.pluginId().isBlank()) {
                        m.put("plugin_id", p.pluginId());
                    }
                    if (p.minPluginVersion() != null && !p.minPluginVersion().isBlank()) {
                        m.put("min_plugin_version", p.minPluginVersion());
                    }
                    return m;
                })
                .toList();

        Map<String, Object> contract = null;
        if (item.contract() != null) {
            java.util.LinkedHashMap<String, Object> c = new java.util.LinkedHashMap<>();
            if (item.contract().schemaUrl() != null && !item.contract().schemaUrl().isBlank()) {
                c.put("schema_url", item.contract().schemaUrl());
            }
            if (item.contract().sha256() != null && !item.contract().sha256().isBlank()) {
                c.put("sha256", item.contract().sha256());
            }
            contract = c.isEmpty() ? null : c;
        }

        return new DomainCatalogItem(item.domain(), item.supportedVersions(), item.recommendedVersion(), constraints, providers, contract);
    }

    /**
     * 域目录响应体。
     */
    public record DomainsCatalogResponse(List<DomainCatalogItem> items) {
    }

    /**
     * 单个域目录项。
     */
    public record DomainCatalogItem(
            String domain,
            List<String> supportedVersions,
            String recommendedVersion,
            Map<String, Object> constraints,
            List<Map<String, Object>> providers,
            Map<String, Object> contract
    ) {
    }
}
