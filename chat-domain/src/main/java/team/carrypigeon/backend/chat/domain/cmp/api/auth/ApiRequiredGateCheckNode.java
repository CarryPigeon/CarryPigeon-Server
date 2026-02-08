package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.RequiredGateCheckRequest;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Required gate check for onboarding.
 * <p>
 * Route: {@code POST /api/gates/required/check} (public)
 * <p>
 * Input: {@link ApiFlowKeys#REQUEST} = {@link RequiredGateCheckRequest}
 * Output: {@link ApiFlowKeys#RESPONSE} = {@link MissingPluginsResponse}
 */
@Slf4j
@LiteflowComponent("ApiRequiredGateCheck")
@RequiredArgsConstructor
public class ApiRequiredGateCheckNode extends AbstractResultNode<ApiRequiredGateCheckNode.MissingPluginsResponse> {

    private final CpApiProperties properties;

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

    public record MissingPluginsResponse(List<String> missingPlugins) {
    }
}
