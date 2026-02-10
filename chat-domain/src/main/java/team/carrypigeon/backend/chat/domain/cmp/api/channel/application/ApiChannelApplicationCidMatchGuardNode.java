package team.carrypigeon.backend.chat.domain.cmp.api.channel.application;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * 频道申请频道一致性守卫节点。
 * <p>
 * 校验决策请求中的 `cid` 与申请记录中的 `cid` 一致，防止跨频道误操作。
 */
@Slf4j
@LiteflowComponent("ApiChannelApplicationCidMatchGuard")
public class ApiChannelApplicationCidMatchGuardNode extends CPNodeComponent {

    /**
     * 校验申请记录所属频道与请求频道一致。
     */
    @Override
    protected void process(CPFlowContext context) {
        Long requestCid = requireContext(context, CPNodeChannelKeys.CHANNEL_INFO_ID);
        CPChannelApplication app = requireContext(context, CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO);
        Long appCid = app.getCid();
        if (appCid == null || !appCid.equals(requestCid)) {
            log.info("ApiChannelApplicationCidMatchGuard mismatch, requestCid={}, appCid={}", requestCid, app.getCid());
            fail(CPProblem.of(CPProblemReason.VALIDATION_FAILED, "validation failed"));
        }
    }
}
