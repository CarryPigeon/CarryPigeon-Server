package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.RequiredGateCheckRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Required Gate 检查节点。
 * <p>
 * 路由：`POST /api/gates/required/check`（公开接口）。
 * <p>
 * 根据客户端已安装插件列表，计算系统要求但客户端缺失的插件集合。
 */
@Slf4j
@LiteflowComponent("ApiRequiredGateCheck")
@RequiredArgsConstructor
public class ApiRequiredGateCheckNode extends AbstractResultNode<ApiRequiredGateCheckNode.MissingPluginsResponse> {

    private final CpApiProperties properties;

    /**
     * 生成缺失插件响应。
     */
    @Override
    protected MissingPluginsResponse build(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof RequiredGateCheckRequest req)) {
            return new MissingPluginsResponse(List.of());
        }
        Set<String> installed = new HashSet<>();
        if (req.client() != null && req.client().installedPlugins() != null) {
            req.client().installedPlugins().forEach(p -> installed.add(p.pluginId()));
        }
        List<String> missing = properties.getApi().getRequiredPlugins().stream()
                .filter(p -> !installed.contains(p))
                .toList();
        MissingPluginsResponse response = new MissingPluginsResponse(missing);
        log.debug("ApiRequiredGateCheck success, missingCount={}", missing.size());
        return response;
    }

    /**
     * 缺失插件响应体。
     */
    public record MissingPluginsResponse(List<String> missingPlugins) {
    }
}
