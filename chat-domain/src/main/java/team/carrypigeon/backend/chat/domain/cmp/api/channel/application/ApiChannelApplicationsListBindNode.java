package team.carrypigeon.backend.chat.domain.cmp.api.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.PageInfo;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ChannelApplicationListRequest;

import java.util.List;
import java.util.Map;

/**
 * 入群申请列表查询绑定节点。
 * <p>
 * 解析 `GET /api/channels/{cid}/applications` 请求中的频道与分页参数。
 */
@Slf4j
@LiteflowComponent("ApiChannelApplicationsListBind")
public class ApiChannelApplicationsListBindNode extends CPNodeComponent {

    /**
     * 解析并绑定申请列表查询参数。
     */
    @Override
    protected void process(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof ChannelApplicationListRequest req)) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
        long cid = parseId(req.cid(), "cid");
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.set(CPNodeValueKeyExtraConstants.PAGE_INFO, new PageInfo(1, 50));
        log.debug("ApiChannelApplicationsListBind success, cid={}", cid);
    }

    /**
     * 解析字符串形式的 ID。
     */
    private long parseId(String str, String field) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            throw new CPProblemException(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed",
                    Map.of("field_errors", List.of(
                            Map.of("field", field, "reason", "invalid", "message", "invalid id")
                    ))));
        }
    }
}
