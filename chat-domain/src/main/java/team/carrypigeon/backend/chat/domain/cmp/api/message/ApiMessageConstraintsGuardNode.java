package team.carrypigeon.backend.chat.domain.cmp.api.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.service.catalog.ApiDomainContractService;
import team.carrypigeon.backend.chat.domain.service.catalog.ApiPluginCatalogIndex;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 消息创建约束校验节点（payload 大小/深度）。
 *
 * <p>使用场景：{@code POST /api/channels/{cid}/messages} 链路中，在“schema 校验”前先做一层快速约束过滤，
 * 避免超大 payload / 超深结构带来的 CPU 与内存风险。
 *
 * <p>失败语义：触发约束时抛出 {@link CPProblemException}，返回 {@code 422 schema_invalid}，
 * 并通过 {@code details.field_errors[]} 指出失败字段与原因。
 */
@Slf4j
@LiteflowComponent("ApiMessageConstraintsGuard")
@RequiredArgsConstructor
public class ApiMessageConstraintsGuardNode extends CPNodeComponent {

    /** Core 文本域（服务端内置、强约束）。 */
    private static final String CORE_TEXT_DOMAIN = "Core:Text";
    /** Core 文本域默认最大 payload（字节，UTF-8）。 */
    private static final int CORE_TEXT_MAX_PAYLOAD_BYTES = 4096;
    /** Core 文本域默认最大结构深度（容器层级）。 */
    private static final int CORE_TEXT_MAX_DEPTH = 10;
    /** 默认 domain_version。 */
    private static final String DEFAULT_DOMAIN_VERSION = "1.0.0";

    /** Domain 合约/约束查询服务（数据来自插件包扫描索引）。 */
    private final ApiDomainContractService contractService;

    /**
     * 执行 payload 大小/深度约束校验。
     *
     * <p>依赖上下文：
     * <ul>
     *   <li>{@link CPNodeMessageKeys#MESSAGE_INFO_DOMAIN}</li>
     *   <li>{@link CPNodeMessageKeys#MESSAGE_INFO_DOMAIN_VERSION}（可选，缺省为 {@code 1.0.0}）</li>
     *   <li>{@link CPNodeMessageKeys#MESSAGE_INFO_DATA}</li>
     * </ul>
     *
     * <p>失败语义：违反约束时抛出 {@link CPProblemException}（HTTP 422）。
     */
    @Override
    protected void process(CPFlowContext context) {
        String domain = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO_DOMAIN);
        String domainVersion = context.get(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN_VERSION);
        if (domainVersion == null || domainVersion.isBlank()) {
            domainVersion = DEFAULT_DOMAIN_VERSION;
        }
        JsonNode data = requireContext(context, CPNodeMessageKeys.MESSAGE_INFO_DATA);

        Constraints c = constraintsOf(domain, domainVersion);
        if (c.maxPayloadBytes() != null) {
            int bytes = data == null ? 0 : data.toString().getBytes(StandardCharsets.UTF_8).length;
            if (bytes > c.maxPayloadBytes()) {
                throw new CPProblemException(CPProblem.of(422, "schema_invalid", "schema invalid",
                        Map.of("field_errors", List.of(
                                Map.of("field", "data", "reason", "too_large", "message", "payload too large")
                        ))));
            }
        }
        if (c.maxDepth() != null) {
            int depth = maxDepth(data);
            if (depth > c.maxDepth()) {
                throw new CPProblemException(CPProblem.of(422, "schema_invalid", "schema invalid",
                        Map.of("field_errors", List.of(
                                Map.of("field", "data", "reason", "too_deep", "message", "payload too deep")
                        ))));
            }
        }
        log.debug("消息创建约束校验：通过：domain={}, domainVersion={}", domain, domainVersion);
    }

    /**
     * 获取某个 domain 在当前版本下的约束。
     *
     * <p>约束来源：
     * <ul>
     *   <li>{@code Core:Text}：使用协议文档给定的固定默认值</li>
     *   <li>插件 domain：来自插件扫描得到的 contract.constraints（见 {@link ApiDomainContractService}）</li>
     * </ul>
     */
    private Constraints constraintsOf(String domain, String domainVersion) {
        // defaults from doc/api
        if (CORE_TEXT_DOMAIN.equals(domain)) {
            return new Constraints(CORE_TEXT_MAX_PAYLOAD_BYTES, CORE_TEXT_MAX_DEPTH);
        }
        ApiPluginCatalogIndex.Constraints c = contractService.constraintsOf(domain, domainVersion);
        return new Constraints(c.maxPayloadBytes(), c.maxDepth());
    }

    /**
     * 计算 JSON 最大嵌套深度。
     *
     * <p>口径：
     * <ul>
     *   <li>空节点：0</li>
     *   <li>标量：1</li>
     *   <li>容器（数组/对象）：1 + 子节点最大深度</li>
     * </ul>
     */
    private int maxDepth(JsonNode node) {
        if (node == null) {
            return 0;
        }
        if (!node.isContainerNode()) {
            return 1;
        }
        int childMax = 0;
        for (JsonNode c : node) {
            childMax = Math.max(childMax, maxDepth(c));
        }
        return 1 + childMax;
    }

    /**
     * 简化后的约束结构。
     *
     * <p>说明：该 record 仅服务于本节点逻辑，避免把外部结构（如配置/索引）直接耦合到校验实现中。
     */
    private record Constraints(Integer maxPayloadBytes, Integer maxDepth) {
    }
}
