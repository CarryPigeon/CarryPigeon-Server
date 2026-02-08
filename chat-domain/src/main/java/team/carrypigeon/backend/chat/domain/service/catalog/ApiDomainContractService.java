package team.carrypigeon.backend.chat.domain.service.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;

import java.util.List;
import java.util.Map;

/**
 * Domain Contract 校验服务。
 * <p>
 * 数据来源：
 * <ul>
 *   <li>插件包扫描索引（{@link ApiPluginCatalogIndex}）</li>
 * </ul>
 * <p>
 * 职责：
 * <ul>
 *   <li>判断某个 domain+version 是否被服务端支持（来自扫描结果）</li>
 *   <li>提供该 domain 的约束（payload bytes / depth）</li>
 *   <li>对非 {@code Core:*} domain 的 payload 执行 schema 校验</li>
 * </ul>
 * <p>
 * Schema 支持范围见 {@link SimpleJsonSchemaValidator} 的实现说明。
 */
@Slf4j
@Service
public class ApiDomainContractService {

    private final ApiPluginCatalogIndex catalogIndex;
    private final SimpleJsonSchemaValidator validator = new SimpleJsonSchemaValidator();

    public ApiDomainContractService(ApiPluginCatalogIndex catalogIndex) {
        this.catalogIndex = catalogIndex;
    }

    /**
     * 判断服务端是否支持某 domain 版本。
     */
    public boolean supports(String domain, String domainVersion) {
        if (domain == null || domain.isBlank() || domainVersion == null || domainVersion.isBlank()) {
            return false;
        }
        for (ApiPluginCatalogIndex.DomainItem d : catalogIndex.snapshot().domainsView()) {
            if (d != null && domain.equals(d.domain())
                    && d.supportedVersions() != null
                    && d.supportedVersions().contains(domainVersion)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取 domain 的约束（bytes/depth）。
     * <p>
     * 注意：当前实现只按 domain 匹配，不区分 domainVersion（约束一般稳定）。
     */
    public ApiPluginCatalogIndex.Constraints constraintsOf(String domain, String domainVersion) {
        if (domain == null || domain.isBlank()) {
            return new ApiPluginCatalogIndex.Constraints(null, null);
        }
        for (ApiPluginCatalogIndex.DomainItem d : catalogIndex.snapshot().domainsView()) {
            if (d != null && domain.equals(d.domain())) {
                return d.constraints() == null ? new ApiPluginCatalogIndex.Constraints(null, null) : d.constraints();
            }
        }
        return new ApiPluginCatalogIndex.Constraints(null, null);
    }

    /**
     * 对 payload 执行 schema 校验；失败抛出 {@code 422 schema_invalid}。
     */
    public void validateOrThrow(String domain, String domainVersion, JsonNode payload) {
        ApiPluginCatalogIndex.ContractFile schemaFile = resolveContractFile(domain, domainVersion);
        if (schemaFile == null || schemaFile.schema() == null) {
            throw new CPProblemException(CPProblem.of(422, "schema_invalid", "schema invalid",
                    Map.of("field_errors", List.of(
                            Map.of("field", "domain_version", "reason", "unsupported", "message", "unsupported domain_version")
                    ))));
        }
        List<String> errors = validator.validate(schemaFile.schema(), payload);
        if (!errors.isEmpty()) {
            throw new CPProblemException(CPProblem.of(422, "schema_invalid", "schema invalid",
                    Map.of("field_errors", List.of(
                            Map.of("field", "data", "reason", "invalid", "message", "schema invalid")
                    ))));
        }
    }

    /**
     * 从索引中解析出某个 domain+version 的 contract schema。
     * <p>
     * 当前策略：从 providers 中找到任意一个 plugin provider，并取其 contractFile。
     */
    private ApiPluginCatalogIndex.ContractFile resolveContractFile(String domain, String domainVersion) {
        ApiPluginCatalogIndex.Snapshot snap = catalogIndex.snapshot();
        if (snap == null) {
            return null;
        }
        for (ApiPluginCatalogIndex.DomainItem d : snap.domainsView()) {
            if (d == null || !domain.equals(d.domain())) {
                continue;
            }
            if (d.supportedVersions() == null || !d.supportedVersions().contains(domainVersion)) {
                continue;
            }
            if (d.providers() == null) {
                continue;
            }
            for (ApiPluginCatalogIndex.Provider p : d.providers()) {
                if (p == null || !"plugin".equals(p.type()) || p.pluginId() == null) {
                    continue;
                }
                ApiPluginCatalogIndex.ContractFile f = snap.contractFile(p.pluginId(), domain, domainVersion);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }
}
